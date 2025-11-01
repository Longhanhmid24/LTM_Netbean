package com.app.chatserver.group_message;

import com.app.chatserver.message.ChatService; // ✅ SỬA: Import ChatService
// import com.app.chatserver.message.dto.MessageReceiveDTO; // (Không dùng DTO nữa)
import com.app.chatserver.model.GroupMessage; // ✅ SỬA: Dùng Model này
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class GroupMessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    public GroupMessageController(SimpMessagingTemplate messagingTemplate, ChatService chatService) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
    }

    /**
     * ✅ SỬA ĐỔI: Nhận GroupMessage (Lớp 1) thay vì DTO Lớp 2
     */
    @MessageMapping("/group.send")
    public void sendGroupMessage(GroupMessage message) {
        System.out.println("📨 Nhận tin nhắn nhóm (Lớp 1) từ client: " + message.getSenderId());
        
        if (message.getGroupId() <= 0) {
             System.err.println("[GroupMessageController] Bỏ qua tin nhắn nhóm không có GroupId");
             return;
        }
        
        // 1. Lưu DB (dùng hàm Lớp 1)
        GroupMessage savedMessage = chatService.saveGroupMessage_Lop1(message);

        if (savedMessage == null) {
            System.err.println("[GroupMessageController] Lỗi khi lưu tin nhắn nhóm (Lớp 1).");
            return;
        }

        // 2. Gửi tới tất cả người trong nhóm qua /topic/group/{groupId}
        messagingTemplate.convertAndSend(
            "/topic/group/" + message.getGroupId(),
            savedMessage // Gửi lại model GroupMessage (đã có ID và timestamp)
        );

        System.out.println("📤 Đã gửi broadcast (Lớp 1) tới /topic/group/" + message.getGroupId());
    }
}