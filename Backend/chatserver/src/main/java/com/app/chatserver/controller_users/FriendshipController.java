package com.app.chatserver.controller_users;

import com.app.chatserver.model.Friendship;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friendships")
@CrossOrigin(origins = "*")
public class FriendshipController {

    private final JdbcTemplate jdbc;

    public FriendshipController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ✅ Gửi lời mời kết bạn
    @PostMapping("/request")
    public ResponseEntity<?> sendFriendRequest(@RequestBody Friendship f) {
        try {
            String checkSql = """
                SELECT COUNT(*) FROM friendships
                WHERE (user_id_1 = ? AND user_id_2 = ?)
                   OR (user_id_1 = ? AND user_id_2 = ?)
            """;
            Integer count = jdbc.queryForObject(checkSql, Integer.class,
                    f.getUserId1(), f.getUserId2(), f.getUserId2(), f.getUserId1());

            if (count != null && count > 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Đã tồn tại quan hệ giữa hai người dùng"));
            }

            String sql = """
                INSERT INTO friendships (user_id_1, user_id_2, status, action_user_id)
                VALUES (?, ?, 'pending', ?)
            """;
            jdbc.update(sql, f.getUserId1(), f.getUserId2(), f.getActionUserId());
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã gửi lời mời kết bạn"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Chấp nhận lời mời kết bạn
    @PutMapping("/accept")
    public ResponseEntity<?> acceptFriend(@RequestBody Friendship f) {
        String sql = """
            UPDATE friendships
            SET status = 'accepted', action_user_id = ?
            WHERE ((user_id_1 = ? AND user_id_2 = ?) OR (user_id_1 = ? AND user_id_2 = ?))
              AND status = 'pending'
        """;
        int rows = jdbc.update(sql, f.getActionUserId(), f.getUserId1(), f.getUserId2(), f.getUserId2(), f.getUserId1());
        if (rows > 0)
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã chấp nhận kết bạn"));
        else
            return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy yêu cầu kết bạn phù hợp"));
    }

    // ✅ Xóa bạn hoặc hủy lời mời
    @DeleteMapping("/remove")
    public ResponseEntity<?> removeFriend(@RequestBody Friendship f) {
        String sql = """
            DELETE FROM friendships
            WHERE (user_id_1 = ? AND user_id_2 = ?)
               OR (user_id_1 = ? AND user_id_2 = ?)
        """;
        int rows = jdbc.update(sql, f.getUserId1(), f.getUserId2(), f.getUserId2(), f.getUserId1());
        if (rows > 0)
            return ResponseEntity.ok(Map.of("removed", true));
        else
            return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy mối quan hệ"));
    }

    // ✅ Danh sách bạn bè đã chấp nhận
    @GetMapping("/{userId}/list")
    public List<Map<String, Object>> getFriends(@PathVariable int userId) {
        String sql = """
            SELECT 
                CASE 
                    WHEN user_id_1 = ? THEN user_id_2 
                    ELSE user_id_1 
                END AS friend_id,
                u.username, u.avatar, f.status
            FROM friendships f
            JOIN users u ON u.id = CASE 
                WHEN f.user_id_1 = ? THEN f.user_id_2 
                ELSE f.user_id_1 
            END
            WHERE (f.user_id_1 = ? OR f.user_id_2 = ?)
              AND f.status = 'accepted'
        """;
        return jdbc.queryForList(sql, userId, userId, userId, userId);
    }

    // ✅ Danh sách lời mời kết bạn (chưa chấp nhận)
    @GetMapping("/{userId}/requests")
    public List<Map<String, Object>> getPendingRequests(@PathVariable int userId) {
        String sql = """
            SELECT 
                f.user_id_1 AS sender_id, 
                u.username, u.avatar, f.status
            FROM friendships f
            JOIN users u ON f.user_id_1 = u.id
            WHERE f.user_id_2 = ? AND f.status = 'pending'
        """;
        return jdbc.queryForList(sql, userId);
    }

    // ✅ Chặn người dùng
    @PutMapping("/block")
    public ResponseEntity<?> blockUser(@RequestBody Friendship f) {
        String sql = """
            UPDATE friendships
            SET status = 'blocked', action_user_id = ?
            WHERE (user_id_1 = ? AND user_id_2 = ?)
               OR (user_id_1 = ? AND user_id_2 = ?)
        """;
        int rows = jdbc.update(sql, f.getActionUserId(), f.getUserId1(), f.getUserId2(), f.getUserId2(), f.getUserId1());
        if (rows > 0)
            return ResponseEntity.ok(Map.of("blocked", true));
        else
            return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy mối quan hệ để chặn"));
    }
}
