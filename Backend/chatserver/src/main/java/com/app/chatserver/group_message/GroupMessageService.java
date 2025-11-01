package com.app.chatserver.group_message;

import com.app.chatserver.model.GroupMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class GroupMessageService {

    private final JdbcTemplate jdbcTemplate;

    public GroupMessageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ✅ Lưu tin nhắn nhóm vào bảng group_messages
    public void saveMessage(GroupMessage message) {
        String sql = """
            INSERT INTO group_messages 
            (group_id, sender_id, message_type, media_url, file_name, content, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try {
            jdbcTemplate.update(sql,
                    message.getGroupId(),
                    message.getSenderId(),
                    message.getMessageType(),
                    message.getMediaUrl(),
                    message.getFileName(),
                    message.getContent(),
                    LocalDateTime.now()
            );

            System.out.println("[GroupMessageService] 💾 Saved message to DB: group=" + message.getGroupId()
                    + ", sender=" + message.getSenderId()
                    + ", type=" + message.getMessageType());
        } catch (Exception ex) {
            System.err.println("[GroupMessageService] ❌ Failed to save group message: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
