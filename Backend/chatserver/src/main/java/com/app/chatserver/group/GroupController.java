package com.app.chatserver.group;

import com.app.chatserver.model.Group;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    /**
     * ✅ API MỚI: Tạo nhóm mới.
     * Nhận {"name": "...", "creator_id": ...}
     */
    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> payload) {
        try {
            String name = (String) payload.get("name");
            Integer creatorId = (Integer) payload.get("creator_id");
            String avatar = (String) payload.get("avatar"); // (Tùy chọn)

            if (name == null || creatorId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Thiếu 'name' hoặc 'creator_id'"));
            }

            Group newGroup = groupService.createGroup(name, creatorId, avatar);
            return ResponseEntity.status(HttpStatus.CREATED).body(newGroup);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ API MỚI: Lấy danh sách nhóm mà một user là thành viên
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Group>> getGroupsForUser(@PathVariable int userId) {
        List<Group> groups = groupService.getGroupsByUserId(userId);
        return ResponseEntity.ok(groups);
    }
    
    /**
     * ✅ API MỚI: Xóa nhóm (chỉ người tạo mới có quyền)
     * @param groupId ID của nhóm cần xóa
     * @param body Body chứa "user_id" (người yêu cầu xóa)
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable int groupId, @RequestBody Map<String, Integer> body) {
        Integer requesterId = body.get("user_id");
        if (requesterId == null) {
             return ResponseEntity.badRequest().body(Map.of("error", "Thiếu 'user_id' (người yêu cầu)"));
        }
        
        try {
            boolean success = groupService.deleteGroup(groupId, requesterId);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa nhóm"));
            } else {
                // Điều này có thể xảy ra nếu nhóm không tồn tại, hoặc user không có quyền
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                     .body(Map.of("error", "Không thể xóa nhóm (không có quyền hoặc nhóm không tồn tại)"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}