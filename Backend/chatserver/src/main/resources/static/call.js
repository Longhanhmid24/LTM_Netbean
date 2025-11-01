/* ===== FIX: BẮT BUỘC DÙNG IP ĐỘNG ===== */
if (location.protocol === 'http:' && location.hostname !== 'localhost' && location.hostname !== '127.0.0.1') {
    // Ép trình duyệt cung cấp mediaDevices dù không phải secure context
    Object.defineProperty(navigator, 'mediaDevices', {
        value: {
            getUserMedia: function(constraints) {
                return new Promise((resolve, reject) => {
                    const getUserMedia =
                        navigator.webkitGetUserMedia ||
                        navigator.mozGetUserMedia ||
                        navigator.msGetUserMedia ||
                        navigator.getUserMedia;

                    if (getUserMedia) {
                        getUserMedia.call(navigator, constraints, resolve, reject);
                    } else {
                        reject(new Error('getUserMedia not supported'));
                    }
                });
            },
            enumerateDevices: () => Promise.resolve([]),
            getSupportedConstraints: () => ({})
        },
        writable: false,
        configurable: false
    });
}

/* ===== Get URL params ===== */
const url = new URL(window.location.href);
const callId = url.searchParams.get("callId");
const userId = Number(url.searchParams.get("userId"));
const peerId = Number(url.searchParams.get("peerId"));
const callType = url.searchParams.get("type") || "video";
document.getElementById("peerLabel").innerText = peerId;

/* ===== WebRTC Setup ===== */
let pc, localStream;

// ✅ FIX: Hàng đợi ICE nếu candidate tới quá sớm
let pendingCandidates = [];

const localVideo = document.getElementById("localVideo");
const remoteVideo = document.getElementById("remoteVideo");

/* ==== WebSocket Signaling (dùng relative path → tự theo IP) ==== */
const socket = new SockJS("/ws/call"); // → ws://[YOUR_IP]:8080/ws/call
const stomp = Stomp.over(socket);

stomp.connect({}, () => {
    stomp.subscribe(`/queue/call/${userId}`, async msg => {
        const signal = JSON.parse(msg.body);
        // console.log("SIGNAL =>", signal);

        // ✅ FIX: Khi có yêu cầu gọi đến, chỉ hiển thị UI & CHỜ offer (không tự offer)
        if (signal.type === "call_request") {
            // console.log("📞 Incoming call — waiting for OFFER…");
            return;
        }

        if (signal.type === "offer") {
            try {
                // ✅ FIX: Người nhận phải init tại đây để bật cam/mic
                await initWebRTC();

                await pc.setRemoteDescription({ type: "offer", sdp: signal.sdp });

                // ✅ FIX: thêm mọi ICE đã đợi trước đó (nếu có)
                for (const cand of pendingCandidates) {
                    try { await pc.addIceCandidate(cand); } catch (e) { console.warn("late ICE add failed:", e); }
                }
                pendingCandidates = [];

                const answer = await pc.createAnswer();
                await pc.setLocalDescription(answer);
                sendSignal("answer", { sdp: answer.sdp });
            } catch (e) {
                console.error("Error handling OFFER:", e);
            }
        }
        else if (signal.type === "answer") {
            // ✅ FIX: Chặn answer rỗng (từ server) làm crash 'v='
            if (!signal.sdp) {
                console.warn("⚠️ Received ANSWER without SDP — skip.");
                return;
            }
            try {
                await pc.setRemoteDescription({ type: "answer", sdp: signal.sdp });
            } catch (e) {
                console.error("Error setting ANSWER:", e);
            }
        }
        else if (signal.type === "candidate") {
            try {
                const candidate = JSON.parse(signal.candidate);

                // ✅ FIX: Nếu chưa có remoteDescription ⇒ tạm xếp hàng
                if (!pc || !pc.remoteDescription) {
                    pendingCandidates.push(candidate);
                } else {
                    await pc.addIceCandidate(candidate);
                }
            } catch (e) {
                console.error("Lỗi khi thêm ICE candidate:", e, signal.candidate);
            }
        }
        else if (signal.type === "hangup") {
            endCallUI();
        }
    });
    
    // ✅ Giữ nguyên logic gốc của bạn
    // Nếu là người gọi (người có userId nhỏ hơn) thì bắt đầu cuộc gọi ngay
    if (userId < peerId) startCall();
});

/* ==== Functions ==== */
async function initWebRTC() {
    if (pc) return; // Đã khởi tạo rồi thì không làm lại

    if (!navigator.mediaDevices?.getUserMedia) {
        alert("Trình duyệt không hỗ trợ camera/microphone.");
        return;
    }

    // Cấu hình ICE Server (nên dùng STUN/TURN server thực tế)
    pc = new RTCPeerConnection({
        iceServers: [{ urls: "stun:stun.l.google.com:19302" }]
    });

    pc.onicecandidate = e => {
        if (e.candidate) sendSignal("candidate", { candidate: JSON.stringify(e.candidate) });
    };

    pc.ontrack = e => {
        remoteVideo.srcObject = e.streams[0];
    };

    // Lấy luồng media
    try {
        localStream = await navigator.mediaDevices.getUserMedia(
            callType === "video" ? { video: true, audio: true } : { audio: true, video: false }
        );

        localStream.getTracks().forEach(t => pc.addTrack(t, localStream));
        localVideo.srcObject = localStream;
        
        // ✅ FIX: Cập nhật trạng thái UI sau khi lấy được localStream
        updateControlsUI();
        
    } catch (e) {
        alert("Không thể truy cập camera/microphone: " + e.name);
        console.error("Lỗi getUserMedia:", e);
    }
}

function updateControlsUI() {
    // Đảm bảo nút Mic hoạt động
    const micTrack = localStream?.getAudioTracks()[0];
    if (micTrack) {
        document.getElementById("btnMic").style.opacity = micTrack.enabled ? "1" : "0.5";
    }

    // Đảm bảo nút Cam hoạt động (nếu là video call)
    if (callType === "video") {
        const camTrack = localStream?.getVideoTracks()[0];
        if (camTrack) {
            document.getElementById("btnCam").style.opacity = camTrack.enabled ? "1" : "0.5";
        }
    } else {
        // Ẩn nút Cam nếu chỉ là audio call
        document.getElementById("btnCam").style.display = "none";
    }
}

async function startCall() {
    // ✅ FIX: bật cam/mic trước rồi mới tạo offer
    await initWebRTC();
    if (!pc) return;
    
    const offer = await pc.createOffer();
    await pc.setLocalDescription(offer);
    sendSignal("offer", { sdp: offer.sdp });
}

function sendSignal(type, data = {}) {
    stomp.send("/app/call.send", {}, JSON.stringify({
        callId,
        callerId: userId,
        receiverId: peerId,
        type,
        ...data
    }));
}

/* ===== UI buttons (đã thêm kiểm tra localStream) ===== */
document.getElementById("btnMic").onclick = () => {
    const track = localStream?.getAudioTracks()[0];
    if (track) {
        track.enabled = !track.enabled;
        document.getElementById("btnMic").style.opacity = track.enabled ? "1" : "0.5";
        // console.log(`Mic status changed: ${track.enabled}`);
    } else {
        console.error("Không tìm thấy Audio Track. LocalStream chưa được khởi tạo.");
    }
};

document.getElementById("btnCam").onclick = () => {
    const track = localStream?.getVideoTracks()[0];
    if (track) {
        track.enabled = !track.enabled;
        document.getElementById("btnCam").style.opacity = track.enabled ? "1" : "0.5";
        // console.log(`Camera status changed: ${track.enabled}`);
    } else {
        console.error("Không tìm thấy Video Track. LocalStream chưa được khởi tạo hoặc không phải Video Call.");
    }
};

document.getElementById("btnEnd").onclick = () => {
    sendSignal("hangup");
    endCallUI();
};

function endCallUI() {
    if (localStream) localStream.getTracks().forEach(t => t.stop());
    if (pc) pc.close();
    
    // Đóng cửa sổ sau một chút độ trễ để đảm bảo tín hiệu được gửi đi
    setTimeout(() => {
        window.close();
    }, 500); 
}
