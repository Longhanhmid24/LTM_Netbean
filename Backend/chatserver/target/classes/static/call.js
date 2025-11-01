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
const isCaller = url.searchParams.get("isCaller") === "true";
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

// ✅ Function cho CALLER
async function startCall() {
    await initWebRTC();
    if (!pc) return;
    
    const offer = await pc.createOffer();
    await pc.setLocalDescription(offer);
    sendSignal("offer", { sdp: offer.sdp });
    console.log("📤 Offer sent");
}

// ✅ Function cho RECEIVER
async function handleIncomingOffer(sdp) {
    try {
        // Đảm bảo WebRTC đã được khởi tạo
        if (!pc) {
            console.error("❌ PeerConnection chưa sẵn sàng!");
            return;
        }
        
        console.log("📥 Received offer, creating answer...");
        
        await pc.setRemoteDescription({ type: "offer", sdp: sdp });

        // Thêm các ICE candidate đang chờ
        for (const cand of pendingCandidates) {
            try { 
                await pc.addIceCandidate(cand);
                console.log("✅ Added pending ICE candidate");
            } catch (e) { 
                console.warn("⚠️ Failed to add pending ICE:", e); 
            }
        }
        pendingCandidates = [];

        const answer = await pc.createAnswer();
        await pc.setLocalDescription(answer);
        sendSignal("answer", { sdp: answer.sdp });
        console.log("📤 Answer sent");
    } catch (e) {
        console.error("❌ Error handling offer:", e);
    }
}

/* ==== WebSocket Event Handler ==== */
stomp.connect({}, async () => {
    console.log("🔌 WebSocket connected");
    
    stomp.subscribe(`/queue/call/${userId}`, async msg => {
        const signal = JSON.parse(msg.body);
        console.log("📨 Received signal:", signal.type);

        if (signal.type === "call_request") {
            return;
        }

        if (signal.type === "offer") {
            await handleIncomingOffer(signal.sdp); // ✅ Gọi function riêng
        } 
        else if (signal.type === "answer") {
            if (!signal.sdp) return;
            try { 
                await pc.setRemoteDescription({ type: "answer", sdp: signal.sdp });
                console.log("✅ Answer received and set");
            } catch (e) { 
                console.error("❌ Error setting answer:", e); 
            }
        } 
        else if (signal.type === "candidate") {
            try {
                const candidate = JSON.parse(signal.candidate);
                if (!pc || !pc.remoteDescription) {
                    pendingCandidates.push(candidate);
                    console.log("⏳ ICE candidate queued (no remote description yet)");
                } else {
                    await pc.addIceCandidate(candidate);
                    console.log("✅ ICE candidate added");
                }
            } catch (e) {
                console.error("❌ Error adding ICE candidate:", e);
            }
        } 
        else if (signal.type === "hangup") {
            endCallUI();
        }
    });

    // ✅ QUAN TRỌNG: Cả 2 đều khởi tạo WebRTC NGAY
    await initWebRTC();
    
    // ✅ Chỉ caller mới tạo offer
    if (isCaller) {
        console.log("👤 Role: CALLER");
        await startCall();
    } else {
        console.log("👤 Role: RECEIVER - waiting for offer...");
    }
});


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