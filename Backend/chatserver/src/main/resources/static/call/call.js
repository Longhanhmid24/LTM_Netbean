/* URL params */
const url = new URL(window.location.href);
const callId = url.searchParams.get("callId");
const userId = Number(url.searchParams.get("userId"));
const peerId = Number(url.searchParams.get("peerId"));
const callType = url.searchParams.get("type") || "video";

document.getElementById("peerLabel").innerText = peerId;

/* WebRTC vars */
let pc, localStream;
const localVideo = document.getElementById("localVideo");
const remoteVideo = document.getElementById("remoteVideo");

/* Ringtone */
const ringOut = new Audio("/sound/ring.mp3");
const ringIn = new Audio("/sound/incoming.mp3");
ringOut.loop = true;
ringIn.loop = true;

const stopSound = () => {
  for (const s of [ringOut, ringIn]) {
    s.pause();
    s.currentTime = 0;
  }
};

/* WS signaling */
const stomp = Stomp.over(new SockJS("/ws/call"));

function sendSignal(type, data = {}) {
  stomp.send("/app/call.send", {}, JSON.stringify({
    callId, callerId: userId, receiverId: peerId, type, ...data
  }));
}

/* WebRTC init */
async function initWebRTC() {
  pc = new RTCPeerConnection();
  pc.onicecandidate = e => e.candidate && sendSignal("candidate",{candidate:JSON.stringify(e.candidate)});
  pc.ontrack = e => remoteVideo.srcObject = e.streams[0];

  localStream = await navigator.mediaDevices.getUserMedia(
    callType === "video" ? { video: true, audio: true } : { audio: true }
  );

  localStream.getTracks().forEach(t => pc.addTrack(t, localStream));
  localVideo.srcObject = localStream;
}

/* Start call */
async function startCall() {
  await initWebRTC();
  const offer = await pc.createOffer();
  await pc.setLocalDescription(offer);
  sendSignal("offer",{ sdp: offer.sdp, callType });
  ringOut.play();
}

/* End call UI */
function endCallUI(){
  stopSound();
  localStream?.getTracks().forEach(t => t.stop());
  pc?.close();
  window.close();
}

/* Signal handler */
stomp.connect({},() => {
  stomp.subscribe(`/queue/call/${userId}`, async msg => {
    const signal = JSON.parse(msg.body);

    if(signal.type==="offer"){
      ringIn.play();
      await initWebRTC();
      await pc.setRemoteDescription({ type:"offer", sdp:signal.sdp });
      const ans = await pc.createAnswer();
      await pc.setLocalDescription(ans);
      sendSignal("answer",{ sdp: ans.sdp });
    }

    else if(signal.type==="answer"){
      stopSound();
      await pc.setRemoteDescription({ type:"answer", sdp: signal.sdp });
    }

    else if(signal.type==="candidate"){
      pc.addIceCandidate(JSON.parse(signal.candidate));
    }

    else if(signal.type==="hangup"){
      endCallUI();
    }
  });
});

// MIC toggle
btnMic.onclick = () => {
  const track = localStream.getAudioTracks()[0];
  track.enabled = !track.enabled;
  btnMic.classList.toggle("off", !track.enabled);
};

// CAMERA toggle
btnCam.onclick = () => {
  const track = localStream.getVideoTracks()[0];
  track.enabled = !track.enabled;
  btnCam.classList.toggle("off", !track.enabled);
};

/* UI buttons */
document.getElementById("btnMic").onclick = () => {
  const track = localStream.getAudioTracks()[0];
  track.enabled = !track.enabled;
  const btn = document.getElementById("btnMic");
  btn.classList.toggle("off");
  btn.textContent = track.enabled ? "🎤" : "🔇";
};

document.getElementById("btnCam").onclick = () => {
  const track = localStream.getVideoTracks()[0];
  track.enabled = !track.enabled;
  const btn = document.getElementById("btnCam");
  btn.classList.toggle("off");
  btn.textContent = track.enabled ? "📷" : "🚫📷";
};

document.getElementById("btnEnd").onclick = () => {
  sendSignal("hangup");
  endCallUI();
};

/* Caller auto-start */
if (userId < peerId) startCall();
