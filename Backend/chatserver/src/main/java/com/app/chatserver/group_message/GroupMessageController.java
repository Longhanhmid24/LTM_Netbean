package com.app.chatserver.group_message;

import com.app.chatserver.model.GroupMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class GroupMessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GroupMessageService groupMessageService;

    public GroupMessageController(SimpMessagingTemplate messagingTemplate, GroupMessageService groupMessageService) {
        this.messagingTemplate = messagingTemplate;
        this.groupMessageService = groupMessageService;
    }

    // ✅ Lắng nghe client gửi tin nhắn nhóm
    @MessageMapping("/group.send")
    public void sendGroupMessage(GroupMessage message) {
        System.out.println("📨 Nhận tin nhắn nhóm từ client: " + message);

        // Lưu DB
        groupMessageService.saveMessage(message);

        // Gửi tới tất cả người trong nhóm qua /topic/group/{groupId}
        messagingTemplate.convertAndSend(
            "/topic/group/" + message.getGroupId(),
            message
        );

        System.out.println("📤 Đã gửi broadcast tới /topic/group/" + message.getGroupId());
    }
}
