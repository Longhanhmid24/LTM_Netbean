let stompClient = null;
const serverIp = window.location.hostname || "localhost";
const protocol = window.location.protocol === "https:" ? "https" : "http";

const userId = Number(prompt("Nhập ID của bạn (1 hoặc 2):"));
const receiverId = userId === 1 ? 2 : 1;

// ✅ Kết nối WebSocket chat
function connect() {
  const socket = new SockJS(`${protocol}://${serverIp}:8080/ws`);
  stompClient = Stomp.over(socket);

  stompClient.connect({}, (frame) => {
    console.log("✅ Connected:", frame);

    stompClient.subscribe(`/queue/messages/${userId}`, (message) => {
      const msg = JSON.parse(message.body);
      appendMessage(msg.senderId === userId ? "me" : "other", msg);
    });
  });
}

// ✅ Gửi tin nhắn text
function sendMessage() {
  const content = document.getElementById("message").value.trim();
  if (!content) return;

  const msg = {
    senderId: userId,
    receiverId: receiverId,
    content: content,
    messageType: "text"
  };

  stompClient.send("/app/chat.send", {}, JSON.stringify(msg));
  appendMessage("me", msg);
  document.getElementById("message").value = "";
}

// ✅ Upload file (ảnh / video / tài liệu)
async function uploadFile(event) {
  const file = event.target.files[0];
  if (!file) return;

  const formData = new FormData();
  formData.append("file", file);

  const progressBar = document.getElementById("progress-bar");
  const progressContainer = document.getElementById("progress-container");
  const progressText = document.getElementById("progress-text");
  progressContainer.style.display = "block";

  try {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", `${protocol}://${serverIp}:8080/api/upload`, true);

    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) {
        const percent = Math.round((e.loaded / e.total) * 100);
        progressBar.style.width = percent + "%";
        progressText.textContent = `Đang tải lên: ${percent}%`;
      }
    };

    xhr.onload = () => {
      progressContainer.style.display = "none";
      progressBar.style.width = "0%";
      progressText.textContent = "";

      if (xhr.status === 200) {
        const res = JSON.parse(xhr.responseText);
        console.log("📁 Upload thành công:", res);

        let fileType = res.fileType;
        if (!fileType || fileType === "undefined") {
          const mime = file.type;
          if (mime.startsWith("image/")) fileType = "image";
          else if (mime.startsWith("video/")) fileType = "video";
          else fileType = "file";
        }

        const msg = {
          senderId: userId,
          receiverId: receiverId,
          messageType: fileType,
          mediaUrl: res.url,
          fileName: res.fileName,
          contentType: res.contentType
        };

        stompClient.send("/app/chat.send", {}, JSON.stringify(msg));
        appendMessage("me", msg);
      } else {
        console.error("❌ Upload thất bại:", xhr.responseText);
        alert("❌ Lỗi upload file!");
      }
    };

    xhr.onerror = () => {
      progressContainer.style.display = "none";
      progressBar.style.width = "0%";
      progressText.textContent = "";
      console.error("❌ Upload lỗi:", xhr.responseText);
      alert("❌ Không thể upload file!");
    };

    xhr.send(formData);
  } catch (err) {
    progressContainer.style.display = "none";
    progressBar.style.width = "0%";
    progressText.textContent = "";
    console.error("Lỗi upload:", err);
    alert("❌ Upload thất bại!");
  }
}

// ✅ Hiển thị tin nhắn
function appendMessage(type, msg) {
  const chatBox = document.getElementById("chat-box");
  const wrapper = document.createElement("div");
  wrapper.classList.add("msg-wrapper", type === "me" ? "me-wrapper" : "other-wrapper");

  const msgDiv = document.createElement("div");
  msgDiv.classList.add("msg", type);

  if (msg.messageType === "text") {
    msgDiv.textContent = msg.content;
  } else if (msg.messageType === "image") {
    msgDiv.innerHTML = `<img src="${msg.mediaUrl}" style="width:230px; border-radius:10px; max-width:100%;">`;
  } else if (msg.messageType === "video") {
    msgDiv.innerHTML = msg.contentType
      ? `<video controls width="250" style="border-radius:10px;"><source src="${msg.mediaUrl}" type="${msg.contentType}"></video>`
      : `<video controls width="250" style="border-radius:10px;"><source src="${msg.mediaUrl}"></video>`;
  } else {
    msgDiv.innerHTML = `
      <a href="${msg.mediaUrl}" target="_blank"
         style="
           display:inline-block;
           text-decoration:none;
           font-weight:500;
           color:${type === "me" ? "#fff" : "#007bff"};
           background:${type === "me" ? "#0056b3" : "#e9ecef"};
           padding:8px 14px;
           border-radius:8px;
           box-shadow:0 1px 3px rgba(0,0,0,0.1);
         ">
         📎 ${msg.fileName}
      </a>`;
  }

  wrapper.appendChild(msgDiv);
  chatBox.appendChild(wrapper);
  chatBox.scrollTop = chatBox.scrollHeight;
}

// ✅ Tải lịch sử tin nhắn
async function loadOldMessages() {
  try {
    const res = await fetch(`${protocol}://${serverIp}:8080/api/messages/${userId}/${receiverId}`);
    if (!res.ok) return;
    const messages = await res.json();

    messages.forEach(m => {
      appendMessage(
        m.sender_id === userId ? "me" : "other",
        {
          senderId: m.sender_id,
          receiverId: m.receiver_id,
          content: m.content,
          messageType: m.message_type,
          mediaUrl: m.media_url,
          fileName: m.file_name
        }
      );
    });
  } catch (e) {
    console.warn("⚠️ Không tải được lịch sử tin nhắn:", e);
  }
}

// === WebRTC CALL ===
let stompCall = null;
let pc = null;
let localStream = null;

function connectCall() {
  const socket = new SockJS(`${protocol}://${serverIp}:8080/ws/call`);
  stompCall = Stomp.over(socket);

  stompCall.connect({}, () => {
    stompCall.subscribe(`/queue/call/${userId}`, async (frame) => {
      const signal = JSON.parse(frame.body);
      console.log("📡 Nhận tín hiệu:", signal);

      if (signal.type === "offer") {
        await initWebRTC(signal.callType);
        await pc.setRemoteDescription({ type: "offer", sdp: signal.sdp });
        const answer = await pc.createAnswer();
        await pc.setLocalDescription(answer);
        sendSignal({ type: "answer", sdp: answer.sdp });
      } else if (signal.type === "answer") {
        await pc.setRemoteDescription({ type: "answer", sdp: signal.sdp });
      } else if (signal.type === "candidate") {
        pc.addIceCandidate(JSON.parse(signal.candidate));
      } else if (signal.type === "hangup") {
        endCall();
      }
    });
  });
}

async function initWebRTC(callType) {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    alert("❌ Trình duyệt không hỗ trợ truy cập camera/mic. Hãy chạy qua HTTP(S) server!");
    throw new Error("MediaDevices not supported");
  }

  pc = new RTCPeerConnection();
  pc.onicecandidate = (e) => {
    if (e.candidate) sendSignal({ type: "candidate", candidate: JSON.stringify(e.candidate) });
  };
  pc.ontrack = (e) => {
    document.getElementById("remoteVideo").srcObject = e.streams[0];
  };

  localStream = await navigator.mediaDevices.getUserMedia(
    callType === "video" ? { video: true, audio: true } : { audio: true }
  );
  localStream.getTracks().forEach(t => pc.addTrack(t, localStream));
  document.getElementById("localVideo").srcObject = localStream;
  document.getElementById("video-container").style.display = "flex";
}

async function startCall(callType) {
  await initWebRTC(callType);
  const offer = await pc.createOffer();
  await pc.setLocalDescription(offer);
  sendSignal({ type: "offer", sdp: offer.sdp, callType });
}

function sendSignal(data) {
  stompCall.send("/app/call.send", {}, JSON.stringify({
    callerId: userId,
    receiverId,
    ...data
  }));
}

function hangUp() {
  sendSignal({ type: "hangup" });
  endCall();
}

function endCall() {
  if (localStream) localStream.getTracks().forEach(t => t.stop());
  if (pc) pc.close();
  document.getElementById("video-container").style.display = "none";
}

connect();
loadOldMessages();
connectCall();
