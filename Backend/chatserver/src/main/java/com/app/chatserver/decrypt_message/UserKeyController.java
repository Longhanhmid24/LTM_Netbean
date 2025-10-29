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

    // ✅ Lưu public key của user
    @PostMapping("/{userId}")
    public void savePublicKey(@PathVariable int userId, @RequestBody Map<String, String> body) {
        String publicKey = body.get("publicKey");
        String sql = """
            INSERT INTO user_keys (user_id, public_key)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE public_key = VALUES(public_key), updated_at = NOW()
        """;
        jdbcTemplate.update(sql, userId, publicKey);
        System.out.println("[UserKeyController]  Saved key for user " + userId);
    }

    // ✅ Lấy public key của người khác
    @GetMapping("/{userId}")
    public Map<String, Object> getPublicKey(@PathVariable int userId) {
        String sql = "SELECT public_key FROM user_keys WHERE user_id = ?";
        return jdbcTemplate.queryForMap(sql, userId);
    }
}
