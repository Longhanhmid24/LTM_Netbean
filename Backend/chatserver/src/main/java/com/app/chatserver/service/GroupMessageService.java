package com.app.chatserver.service;

import com.app.chatserver.model.Group;
import com.app.chatserver.model.GroupMessage;
import com.app.chatserver.model.User;
import com.app.chatserver.repository.GroupMessageRepository;
import com.app.chatserver.repository.GroupRepository;
import com.app.chatserver.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GroupMessageService {

    private final GroupMessageRepository groupMessageRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupMessageService(GroupMessageRepository groupMessageRepository, GroupRepository groupRepository, UserRepository userRepository) {
        this.groupMessageRepository = groupMessageRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    public GroupMessage sendMessage(Integer groupId, Integer senderId, GroupMessage.MessageType messageType, String content, String mediaUrl, String fileName) {
        Group group = groupRepository.findById(groupId).orElse(null);
        User sender = userRepository.findActiveUserById(senderId);
        if (group != null && sender != null) {
            GroupMessage message = new GroupMessage(group, sender, messageType, content, mediaUrl, fileName);
            message.setTimestamp(LocalDateTime.now());
            return groupMessageRepository.save(message);
        }
        return null;
    }

    public List<GroupMessage> getGroupMessages(Integer groupId) {
        Group group = groupRepository.findById(groupId).orElse(null);
        return group != null ? groupMessageRepository.findMessagesByGroup(group) : List.of();
    }
}