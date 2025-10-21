package com.app.chatserver.controller;

import com.app.chatserver.model.PrivateMessage;
import com.app.chatserver.service.PrivateMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/private-messages")
@CrossOrigin(origins = "*")
public class PrivateMessageController {

    private final PrivateMessageService privateMessageService;

    public PrivateMessageController(PrivateMessageService privateMessageService) {
        this.privateMessageService = privateMessageService;
    }

    @PostMapping
    public ResponseEntity<PrivateMessage> sendMessage(@RequestParam Integer senderId, @RequestParam Integer receiverId, @RequestParam String messageType, @RequestParam(required = false) String content, @RequestParam(required = false) String mediaUrl, @RequestParam(required = false) String fileName) {
        PrivateMessage.MessageType type = PrivateMessage.MessageType.valueOf(messageType.toUpperCase());
        PrivateMessage message = privateMessageService.sendMessage(senderId, receiverId, type, content, mediaUrl, fileName);
        return message != null ? ResponseEntity.ok(message) : ResponseEntity.badRequest().build();
    }

    @GetMapping
    public ResponseEntity<List<PrivateMessage>> getMessagesBetweenUsers(@RequestParam Integer userId1, @RequestParam Integer userId2) {
        return ResponseEntity.ok(privateMessageService.getMessagesBetweenUsers(userId1, userId2));
    }
}