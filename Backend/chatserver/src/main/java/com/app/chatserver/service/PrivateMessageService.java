package com.app.chatserver.service;

import com.app.chatserver.model.PrivateMessage;
import com.app.chatserver.model.User;
import com.app.chatserver.repository.PrivateMessageRepository;
import com.app.chatserver.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PrivateMessageService {

    private final PrivateMessageRepository privateMessageRepository;
    private final UserRepository userRepository;

    public PrivateMessageService(PrivateMessageRepository privateMessageRepository, UserRepository userRepository) {
        this.privateMessageRepository = privateMessageRepository;
        this.userRepository = userRepository;
    }

    public PrivateMessage sendMessage(Integer senderId, Integer receiverId, PrivateMessage.MessageType messageType, String content, String mediaUrl, String fileName) {
        User sender = userRepository.findActiveUserById(senderId);
        User receiver = userRepository.findActiveUserById(receiverId);
        if (sender != null && receiver != null) {
            PrivateMessage message = new PrivateMessage(sender, receiver, messageType, content, mediaUrl, fileName);
            message.setTimestamp(LocalDateTime.now());
            return privateMessageRepository.save(message);
        }
        return null;
    }

    public List<PrivateMessage> getMessagesBetweenUsers(Integer userId1, Integer userId2) {
        User user1 = userRepository.findActiveUserById(userId1);
        User user2 = userRepository.findActiveUserById(userId2);
        if (user1 != null && user2 != null) {
            return privateMessageRepository.findMessagesBetweenUsers(user1, user2);
        }
        return List.of();
    }
}