package com.app.chatserver.controller_users;

import com.app.chatserver.model.User;
import com.app.chatserver.Repository.UserRepository; // Import UserRepository
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Base64; // Import Base64
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

    /**
     * ✅ SỬA ĐỔI: Login và trả về E2EE fields
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String usernameOrSdt = request.get("username");
            String password = request.get("password");

            if (usernameOrSdt == null || password == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Thiếu username hoặc password"));

            // 🔍 Tìm user bằng hàm query mới
            User user = userRepository.findActiveUserByUsernameOrSdt(usernameOrSdt)
                    .orElse(null);

            if (user == null)
                return ResponseEntity.status(401).body(Map.of("error", "Người dùng không tồn tại hoặc đã bị khóa"));

            // 🔑 So sánh password plaintext và hash
            if (!passwordEncoder.matches(password, user.getPassword()))
                return ResponseEntity.status(401).body(Map.of("error", "Sai mật khẩu"));

            // 🟢 Thành công → trả thông tin cơ bản + E2EE fields
            // Chuyển đổi BLOB (byte[]) của private key thành Base64 String
            String encPrivateKeyString = Base64.getEncoder().encodeToString(user.getEncPrivateKey());
            
            return ResponseEntity.ok(Map.ofEntries(
                    Map.entry("message", "Đăng nhập thành công"),
                    Map.entry("userId", user.getId()),
                    Map.entry("username", user.getUsername()),
                    Map.entry("sdt", user.getSdt() != null ? user.getSdt() : ""),
                    Map.entry("avatar", user.getAvatar() != null ? user.getAvatar() : ""),
                    // Trả về E2EE fields
                    Map.entry("encPrivateKey", encPrivateKeyString), // (Base64 String)
                    Map.entry("salt", user.getSalt()),             // (Base64 String)
                    Map.entry("iv", user.getIv())                  // (Base64 String)
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi server: " + e.getMessage()));
        }
    }
}