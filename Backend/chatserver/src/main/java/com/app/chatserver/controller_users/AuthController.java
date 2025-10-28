package com.app.chatserver.controller_users;

import com.app.chatserver.model.User;
import com.app.chatserver.Repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ✅ LOGIN — kiểm tra username/sdt + password hash
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String usernameOrSdt = request.get("username");
            String password = request.get("password");

            if (usernameOrSdt == null || password == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Thiếu username hoặc password"));

            // 🔍 Tìm user theo username hoặc sdt
            User user = userRepository.findAll().stream()
                    .filter(u -> (u.getUsername().equals(usernameOrSdt) || u.getSdt().equals(usernameOrSdt))
                            && u.getDeletedAt() == null
                            && !u.getIsSuspended())
                    .findFirst()
                    .orElse(null);

            if (user == null)
                return ResponseEntity.status(401).body(Map.of("error", "Người dùng không tồn tại"));

            // 🔑 So sánh password plaintext và hash
            if (!passwordEncoder.matches(password, user.getPassword()))
                return ResponseEntity.status(401).body(Map.of("error", "Sai mật khẩu"));

            // 🟢 Thành công → trả thông tin cơ bản
                return ResponseEntity.ok(Map.ofEntries(
                    Map.entry("message", "Đăng nhập thành công"),
                    Map.entry("userId", user.getId()),
                    Map.entry("username", user.getUsername()),
                    Map.entry("sdt", user.getSdt() != null ? user.getSdt() : ""),
                    Map.entry("avatar", user.getAvatar() != null ? user.getAvatar() : "")
                ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi server: " + e.getMessage()));
        }
    }
}
