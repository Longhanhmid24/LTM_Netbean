package com.app.chatserver.controller;

import com.app.chatserver.model.GroupMessage;
import com.app.chatserver.service.GroupMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/group-messages")
@CrossOrigin(origins = "*")
public class GroupMessageController {

    private final GroupMessageService groupMessageService;

    public GroupMessageController(GroupMessageService groupMessageService) {
        this.groupMessageService = groupMessageService;
    }

    @PostMapping
    public ResponseEntity<GroupMessage> sendMessage(@RequestParam Integer groupId, @RequestParam Integer senderId, @RequestParam String messageType, @RequestParam(required = false) String content, @RequestParam(required = false) String mediaUrl, @RequestParam(required = false) String fileName) {
        GroupMessage.MessageType type = GroupMessage.MessageType.valueOf(messageType.toUpperCase());
        GroupMessage message = groupMessageService.sendMessage(groupId, senderId, type, content, mediaUrl, fileName);
        return message != null ? ResponseEntity.ok(message) : ResponseEntity.badRequest().build();
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<List<GroupMessage>> getGroupMessages(@PathVariable Integer groupId) {
        return ResponseEntity.ok(groupMessageService.getGroupMessages(groupId));
    }
}