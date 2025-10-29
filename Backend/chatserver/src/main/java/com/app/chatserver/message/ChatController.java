package com.app.chatserver.message;

import com.app.chatserver.message.dto.MessageReceiveDTO; // ✅ IMPORT DTO MỚI
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    public ChatController(SimpMessagingTemplate messagingTemplate, ChatService chatService) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
    }

    /**
     * ✅ SỬA ĐỔI: Nhận DTO E2EE Lớp 2
     */
    @MessageMapping("/chat.send")
    public void sendMessage(MessageReceiveDTO messageDto) {
        System.out.println("📨 Đã nhận tin nhắn 1:1 E2EE Lớp 2 từ client: sender=" + messageDto.getSenderId());

        // 1. Lưu tin nhắn vào DB (logic E2EE Lớp 2)
        ChatMessage savedMessage = chatService.saveEncryptedMessage(messageDto);

        if (savedMessage == null) {
            System.err.println("[ChatController] Lỗi khi lưu tin nhắn E2EE. Hủy gửi.");
            return;
        }
        
        // 2. Gửi tin nhắn (đã mã hóa) tới người nhận
        messagingTemplate.convertAndSend(
            "/queue/messages/" + messageDto.getReceiverId(),
            savedMessage // Gửi lại đối tượng ChatMessage (đã mã hóa)
        );
        
        // 3. Gửi LẠI cho chính NGƯỜI GỬI (để đồng bộ thiết bị)
        // Client sẽ cần lấy key của chính mình (ví dụ: "1")
        savedMessage.setEncSessionKey(messageDto.getKeys().get(String.valueOf(messageDto.getSenderId())));
        
        messagingTemplate.convertAndSend(
            "/queue/messages/" + messageDto.getSenderId(),
            savedMessage 
        );

        System.out.println("📤 Đã gửi E2EE (1:1) tới /queue/messages/" + messageDto.getReceiverId() + " và " + messageDto.getSenderId());
    }
}