package com.app.chatserver.message;

import com.app.chatserver.message.dto.MessageReceiveDTO;
import com.app.chatserver.model.GroupMessage; // ✅ IMPORT MỚI
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId; 
import java.util.Base64;
import java.util.Map;
import java.nio.charset.StandardCharsets; // ✅ IMPORT MỚI

@Service
public class ChatService {

    private final JdbcTemplate jdbcTemplate;

    public ChatService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * ✅ LOGIC LỚP 2: Lưu tin nhắn E2EE 1:1
     * (Hàm này giữ nguyên)
     */
    @Transactional
    public ChatMessage saveEncryptedMessage(MessageReceiveDTO dto) {
        
        byte[] contentCipherBytes;
        try {
            contentCipherBytes = Base64.getDecoder().decode(dto.getContentCipher());
        } catch (Exception e) {
             System.err.println("[ChatService] Lỗi decode content_cipher (Base64): " + e.getMessage());
             return null;
        }

        // --- 1. Lưu vào bảng 'messages' ---
        String sqlMessages = """
            INSERT INTO messages 
            (sender_id, group_id, message_type, content_cipher, content_iv, timestamp)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();

        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sqlMessages, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, dto.getSenderId());
                
                if (dto.getGroupId() != null) {
                    ps.setInt(2, dto.getGroupId());
                } else {
                    ps.setNull(2, java.sql.Types.INTEGER);
                }
                
                ps.setString(3, dto.getMessageType());
                ps.setBytes(4, contentCipherBytes); // Lưu dạng BLOB
                ps.setString(5, dto.getContentIv()); // Lưu IV (Base64 String)
                ps.setTimestamp(6, Timestamp.valueOf(now));
                return ps;
            }, keyHolder);

        } catch (Exception e) {
            System.err.println("[ChatService] Lỗi INSERT vào 'messages': " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        Long newMessageId = null;
        if (keyHolder.getKey() != null) {
             newMessageId = keyHolder.getKey().longValue();
        }
        if (newMessageId == null || newMessageId == 0) {
             System.err.println("[ChatService] Không lấy được ID tin nhắn vừa tạo.");
             return null;
        }

        // --- 2. Lưu vào bảng 'message_keys' ---
        String sqlKeys = """
            INSERT INTO message_keys
            (msg_id, recipient_id, enc_session_key)
            VALUES (?, ?, ?)
            """;
        
        if (dto.getKeys() == null || dto.getKeys().isEmpty()) {
             System.err.println("[ChatService] Lỗi: Tin nhắn E2EE không có danh sách 'keys'.");
             throw new RuntimeException("Tin nhắn E2EE không có danh sách 'keys'");
        }
        
        try {
            for (Map.Entry<String, String> entry : dto.getKeys().entrySet()) {
                int recipientId = Integer.parseInt(entry.getKey());
                String encSessionKey = entry.getValue();
                jdbcTemplate.update(sqlKeys, newMessageId, recipientId, encSessionKey);
            }
        } catch (Exception e) {
             System.err.println("[ChatService] Lỗi INSERT vào 'message_keys': " + e.getMessage());
             e.printStackTrace();
             throw new RuntimeException("Lỗi lưu message_keys", e); // Báo lỗi để rollback
        }

        System.out.println("[ChatService] 💾 Đã lưu E2EE Lớp 2 (Msg ID: " + newMessageId + ") cho " + dto.getKeys().size() + " người nhận.");

        // --- 3. Tạo đối tượng ChatMessage (đã mã hóa) để gửi lại ---
        ChatMessage responseMsg = new ChatMessage();
        responseMsg.setId(newMessageId);
        responseMsg.setSenderId(dto.getSenderId());
        responseMsg.setReceiverId(dto.getReceiverId() != null ? dto.getReceiverId() : 0);
        responseMsg.setMessageType(dto.getMessageType());
        responseMsg.setContent(dto.getContentCipher()); // Gửi cipher (Base64)
        responseMsg.setContentIv(dto.getContentIv()); // Gửi IV (Base64)
        responseMsg.setTimestamp(now); 
        
        if (dto.getReceiverId() != null) {
             responseMsg.setEncSessionKey(dto.getKeys().get(String.valueOf(dto.getReceiverId())));
        }

        return responseMsg;
    }
    
    /**
     * ✅ HÀM MỚI (LỚP 1): Lưu tin nhắn nhóm (không phải E2EE Lớp 2)
     * (Hàm này lưu mediaUrl/fileName thay vì contentCipher)
     */
    public GroupMessage saveGroupMessage_Lop1(GroupMessage message) {
        
        String sql = """
            INSERT INTO messages 
            (sender_id, group_id, message_type, content_cipher, content_iv, timestamp)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
            
        String contentToSave = message.getContent();
        
        // Nếu là file, Client (Lớp 1) gửi mediaUrl và fileName
        // Chúng ta phải lưu chúng vào cột content_cipher
        if (!"text".equals(message.getMessageType())) {
            contentToSave = String.format("{\"url\":\"%s\",\"fileName\":\"%s\"}", 
                                message.getMediaUrl(), message.getFileName());
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();

        try {
            byte[] contentBytes = (contentToSave != null) ? 
                                  contentToSave.getBytes(StandardCharsets.UTF_8) : 
                                  new byte[0];

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, message.getSenderId());
                ps.setInt(2, message.getGroupId());
                ps.setString(3, message.getMessageType());
                ps.setBytes(4, contentBytes); // Lưu content (hoặc JSON file) vào cipher
                ps.setString(5, ""); // Không có IV cho Lớp 1
                ps.setTimestamp(6, Timestamp.valueOf(now));
                return ps;
            }, keyHolder);
            
            Long newId = (keyHolder.getKey() != null) ? keyHolder.getKey().longValue() : null;
            if (newId != null) {
                message.setId(newId);
            }
            message.setTimestamp(now); // Đặt timestamp
            
            System.out.println("[ChatService] 💾 Đã lưu tin nhắn NHÓM (Lớp 1) vào DB: group=" + message.getGroupId());
            return message;

        } catch (Exception ex) {
            System.err.println("[ChatService] ❌ Lỗi lưu tin nhắn nhóm (Lớp 1): " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }
}