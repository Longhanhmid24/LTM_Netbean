package com.app.chatserver.media_file;

import com.fasterxml.jackson.databind.JsonNode; // ✅ IMPORT MỚI
import com.fasterxml.jackson.databind.ObjectMapper; // ✅ IMPORT MỚI
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Base64; // ✅ IMPORT MỚI
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets; // ✅ IMPORT MỚI

@RestController
@RequestMapping("/api") 
@CrossOrigin(origins = "*")
public class ChatHistoryController {

    private final JdbcTemplate jdbcTemplate;

    public ChatHistoryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 📩 LẤY LỊCH SỬ 1:1 (E2EE LỚP 2)
     * (Giữ nguyên)
     */
    @GetMapping("/messages/{userId}/{friendId}")
    public List<Map<String, Object>> getMessages(@PathVariable int userId, @PathVariable int friendId) {
        
        System.out.println("[ChatHistory] Lấy lịch sử Lớp 2 cho User " + userId + " và Friend " + friendId);
        String sql = """
            SELECT 
                m.id, m.sender_id, m.group_id, m.content_cipher, m.content_iv, 
                m.message_type, m.timestamp, mk.enc_session_key
            FROM messages m
            JOIN message_keys mk ON m.id = mk.msg_id
            WHERE
                m.group_id IS NULL AND mk.recipient_id = ?
                AND m.id IN (
                    SELECT msg_id FROM message_keys WHERE recipient_id = ?
                    INTERSECT
                    SELECT msg_id FROM message_keys WHERE recipient_id = ?
                )
            ORDER BY m.timestamp ASC
        """;
        return jdbcTemplate.queryForList(sql, userId, userId, friendId);
    }
    
    /**
     * 👨‍👩‍👧‍👦 API MỚI (LỚP 1): LẤY LỊCH SỬ NHÓM
     */
    @GetMapping("/group-messages/{groupId}")
    public List<Map<String, Object>> getGroupMessages(@PathVariable int groupId) {
        System.out.println("[ChatHistory] Lấy lịch sử Lớp 1 cho Group " + groupId);
        
        String sql = """
            SELECT 
                m.id, 
                m.sender_id, 
                m.group_id, 
                m.content_cipher, 
                m.content_iv, 
                m.message_type, 
                m.timestamp
            FROM messages m
            WHERE m.group_id = ?
            ORDER BY m.timestamp ASC
        """;
        
        // Cần ObjectMapper để parse JSON (nếu là file)
        ObjectMapper objectMapper = new ObjectMapper();

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            java.util.Map<String, Object> row = new java.util.HashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("sender_id", rs.getInt("sender_id"));
            row.put("group_id", rs.getInt("group_id"));
            
            byte[] cipherBytes = rs.getBytes("content_cipher");
            String messageType = rs.getString("message_type");
            
            row.put("message_type", messageType);
            row.put("timestamp", rs.getTimestamp("timestamp").toLocalDateTime());
            
            // Xử lý nội dung dựa trên Lớp 1
            if ("text".equals(messageType)) {
                // Nếu là text, nó có thể là plaintext hoặc E2EE Lớp 1 (AES chung)
                // Client Lớp 1 (GroupChatForm) mong đợi 'content'
                row.put("content", new String(cipherBytes, StandardCharsets.UTF_8));
                row.put("media_url", null);
                row.put("file_name", null);
            } else {
                // Nếu là file, Lớp 1 lưu JSON {"url":"...", "fileName":"..."}
                try {
                     String payload = new String(cipherBytes, StandardCharsets.UTF_8);
                     JsonNode root = objectMapper.readTree(payload);
                     row.put("media_url", root.path("url").asText(null));
                     row.put("file_name", root.path("fileName").asText(null));
                     row.put("content", null); // Không phải text
                 } catch (Exception e) {
                     // Nếu parse lỗi (có thể là E2EE Lớp 1?)
                     System.err.println("Lỗi parse payload file Lớp 1: " + e.getMessage());
                     row.put("content", Base64.getEncoder().encodeToString(cipherBytes));
                 }
            }

            return row;
        }, groupId);
    }
}