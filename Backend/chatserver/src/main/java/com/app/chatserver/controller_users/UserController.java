package com.app.chatserver.controller_users;

import com.app.chatserver.model.RegisterRequest; // Import DTO
import com.app.chatserver.model.User;
import com.app.chatserver.users.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.Base64; // Import Base64
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * ✅ SỬA ĐỔI: Nhận RegisterRequest DTO thay vì User
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody RegisterRequest request) {
        try {
            User saved = userService.createUser(request);
            // Trả về thông tin cơ bản, không trả về password hay E2EE keys
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đăng ký thành công",
                    "userId", saved.getId(),
                    "username", saved.getUsername()
            ));
        } catch (IllegalArgumentException e) {
            // Lỗi nghiệp vụ (SĐT trùng, password trống, v.v.)
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi máy chủ: " + e.getMessage()));
        }
    }

    // 🔹 GET - Lấy tất cả user (Giữ nguyên)
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // 🔹 GET - Lấy user theo id (Giữ nguyên)
    @GetMapping("/{id}")
    public User getUserById(@PathVariable int id) {
        return userService.getUserById(id);
    }

    // 🔹 PUT - Cập nhật user (Giữ nguyên)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable int id, @RequestBody User user) {
        User updated = userService.updateUser(id, user);
        if (updated != null) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật thành công"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy user"));
        }
    }

    // 🔹 DELETE - Xóa user (Giữ nguyên)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable int id) {
        boolean result = userService.deleteUser(id);
        if (result)
            return ResponseEntity.ok(Map.of("deleted", true));
        else
            return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy user để xóa"));
    }
}