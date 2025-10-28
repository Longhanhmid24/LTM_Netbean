package com.app.chatserver.controller_users;

import com.app.chatserver.model.User;
import com.app.chatserver.users.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

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

    // 🟢 POST - Tạo user mới
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User saved = userService.createUser(user);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đăng ký thành công",
                    "userId", saved.getId(),
                    "username", saved.getUsername()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi máy chủ: " + e.getMessage()));
        }
    }

    // 🔹 GET - Lấy tất cả user
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // 🔹 GET - Lấy user theo id
    @GetMapping("/{id}")
    public User getUserById(@PathVariable int id) {
        return userService.getUserById(id);
    }

    // 🔹 PUT - Cập nhật user
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable int id, @RequestBody User user) {
        User updated = userService.updateUser(id, user);
        if (updated != null) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật thành công"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy user"));
        }
    }

    // 🔹 DELETE - Xóa user
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable int id) {
        boolean result = userService.deleteUser(id);
        if (result)
            return ResponseEntity.ok(Map.of("deleted", true));
        else
            return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy user để xóa"));
    }
}
