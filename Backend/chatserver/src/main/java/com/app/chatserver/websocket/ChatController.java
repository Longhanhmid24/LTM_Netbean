package com.app.chatserver.websocket;

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

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessage message) {
        System.out.println("📨 Đã nhận tin nhắn từ client: " + message);

        chatService.saveMessage(message);

        messagingTemplate.convertAndSend(
            "/queue/messages/" + message.getReceiverId(),
            message
        );
        System.out.println("📤 Đã gửi lại tới /queue/messages/" + message.getReceiverId());
    }
}
