package com.app.chatserver.message;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class ChatHistoryController {

    private final JdbcTemplate jdbcTemplate;

    public ChatHistoryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 📩 Lấy lịch sử giữa 2 user
    @GetMapping("/{userId}/{friendId}")
    public List<Map<String, Object>> getMessages(@PathVariable int userId, @PathVariable int friendId) {
        String sql = """
            SELECT * FROM private_messages
            WHERE (sender_id = ? AND receiver_id = ?)
               OR (sender_id = ? AND receiver_id = ?)
            ORDER BY timestamp ASC
        """;
        return jdbcTemplate.queryForList(sql, userId, friendId, friendId, userId);
    }
}
