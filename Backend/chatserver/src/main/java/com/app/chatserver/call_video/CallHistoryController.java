package com.app.chatserver.call_video;

import com.app.chatserver.model.CallRequest; // 🟢 THÊM DÒNG NÀY
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calls")
@CrossOrigin(origins = "*")
public class CallHistoryController {

    private final JdbcTemplate jdbcTemplate;

    public CallHistoryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ✅ Bắt đầu cuộc gọi
    @PostMapping("/start")
    public void startCall(@RequestBody CallRequest call) {
        String sql = """
            INSERT INTO private_calls (caller_id, receiver_id, call_type, status, start_time)
            VALUES (?, ?, ?, 'ringing', ?)
        """;
        jdbcTemplate.update(sql,
                call.getCallerId(),
                call.getReceiverId(),
                call.getCallType(),
                LocalDateTime.now()
        );
        System.out.println("[CallHistoryController] 📞 Start call: " + call);
    }

    // ✅ Kết thúc cuộc gọi
    @PutMapping("/end/{id}")
    public void endCall(@PathVariable long id) {
        String sql = """
            UPDATE private_calls
            SET status='completed', end_time=NOW()
            WHERE id=?
        """;
        jdbcTemplate.update(sql, id);
        System.out.println("[CallHistoryController] 📴 End call ID: " + id);
    }

    // ✅ Lấy lịch sử cuộc gọi của người dùng
    @GetMapping("/{userId}")
    public List<Map<String, Object>> getUserCallHistory(@PathVariable int userId) {
        String sql = """
            SELECT * FROM private_calls
            WHERE caller_id = ? OR receiver_id = ?
            ORDER BY start_time DESC
        """;
        return jdbcTemplate.queryForList(sql, userId, userId);
    }
}
