package com.app.chatserver.decrypt_message;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/keys")
@CrossOrigin(origins = "*")
public class UserKeyController {

    private final JdbcTemplate jdbcTemplate;

    public UserKeyController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * ✅ SỬA LỖI: Cập nhật public_key trong bảng USERS
     */
    @PostMapping("/{userId}")
    public void savePublicKey(@PathVariable int userId, @RequestBody Map<String, String> body) {
        String publicKey = body.get("publicKey");
        
        // Sửa SQL: UPDATE bảng 'users'
        String sql = """
            UPDATE users 
            SET public_key = ?, updated_at = NOW()
            WHERE id = ?
        """;
        
        jdbcTemplate.update(sql, publicKey, userId);
        System.out.println("[UserKeyController] 🔑 Updated public_key in USERS table for user " + userId);
    }

    /**
     * ✅ SỬA LỖI: Lấy public_key từ bảng USERS
     */
    @GetMapping("/{userId}")
    public Map<String, Object> getPublicKey(@PathVariable int userId) {
        // Sửa SQL: SELECT từ bảng 'users'
        String sql = "SELECT public_key FROM users WHERE id = ?";
        try {
            return jdbcTemplate.queryForMap(sql, userId);
        } catch (Exception e) {
             System.err.println("[UserKeyController] Không tìm thấy key cho user " + userId + ". Lỗi: " + e.getMessage());
             return Map.of("error", "Không tìm thấy key");
        }
    }
}