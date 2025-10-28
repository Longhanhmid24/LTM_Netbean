package com.app.chatserver.group_member;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group-members")
@CrossOrigin(origins = "*")
public class GroupMemberController {

    private final JdbcTemplate jdbc;

    public GroupMemberController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ✅ Thêm thành viên vào nhóm
    @PostMapping("/add")
    public ResponseEntity<?> addMember(@RequestBody Map<String, Object> body) {
        Integer groupId = (Integer) body.get("groupId");
        Integer memberId = (Integer) body.get("memberId");
        String role = (String) body.getOrDefault("role", "member");

        if (groupId == null || memberId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "groupId và memberId là bắt buộc"));
        }

        String sql = "INSERT INTO group_members (group_id, member_id, role) VALUES (?, ?, ?)";
        try {
            jdbc.update(sql, groupId, memberId, role);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Lấy danh sách thành viên theo groupId
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupMembers(@PathVariable int groupId) {
        String sql = """
            SELECT gm.group_id, gm.member_id, gm.role, u.username, u.avatar
            FROM group_members gm
            JOIN users u ON gm.member_id = u.id
            WHERE gm.group_id = ?
        """;
        List<Map<String, Object>> members = jdbc.queryForList(sql, groupId);
        return ResponseEntity.ok(members);
    }

    // ✅ Xoá thành viên khỏi nhóm
    @DeleteMapping("/{groupId}/{memberId}")
    public ResponseEntity<?> removeMember(@PathVariable int groupId, @PathVariable int memberId) {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND member_id = ?";
        jdbc.update(sql, groupId, memberId);
        return ResponseEntity.ok(Map.of("removed", true));
    }
}
