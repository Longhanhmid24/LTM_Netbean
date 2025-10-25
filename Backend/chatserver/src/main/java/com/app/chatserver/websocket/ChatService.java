package com.app.chatserver.websocket;

import com.app.chatserver.model.User;
import com.app.chatserver.Repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ChatService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    public ChatService(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    public void saveMessage(ChatMessage message) {
        String sql = """
            INSERT INTO private_messages 
            (sender_id, receiver_id, message_type, media_url, file_name, content, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql,
                message.getSenderId(),
                message.getReceiverId(),
                message.getMessageType(),
                message.getMediaUrl(),
                message.getFileName(),
                message.getContent(),
                LocalDateTime.now()
        );
    }
}
