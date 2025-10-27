package com.app.chatserver.message;

import com.app.chatserver.model.User;
import com.app.chatserver.Repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// ✅ THÊM DÒNG NÀY
import com.app.chatserver.message.ChatMessage;

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
        try {
            jdbcTemplate.update(sql,
                    message.getSenderId(),
                    message.getReceiverId(),
                    message.getMessageType(),
                    message.getMediaUrl(),
                    message.getFileName(),
                    message.getContent(),
                    LocalDateTime.now()
            );
            System.out.println("[ChatService] Saved message to DB: sender=" + message.getSenderId()
                    + " receiver=" + message.getReceiverId()
                    + " type=" + message.getMessageType());
        } catch (Exception ex) {
            System.err.println("[ChatService] Failed to save message to DB: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
