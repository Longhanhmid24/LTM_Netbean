package com.app.chatserver.users;

import com.app.chatserver.model.RegisterRequest; // ✅ Phải import DTO
import com.app.chatserver.model.User;
import com.app.chatserver.Repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64; // ✅ Import Base64
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * ✅ SỬA LỖI: Tạo user mới VÀ lưu các trường E2EE
     */
    public User createUser(RegisterRequest request) {
        // 1. Kiểm tra SĐT hoặc Username đã tồn tại chưa
        if (userRepository.existsBySdt(request.getSdt())) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
             throw new IllegalArgumentException("Username đã được sử dụng");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
             throw new IllegalArgumentException("Mật khẩu không được để trống");
        }
        // 2. ✅ KIỂM TRA E2EE FIELDS
        if (request.getPublicKey() == null || request.getEncPrivateKey() == null ||
            request.getSalt() == null || request.getIv() == null) {
            throw new IllegalArgumentException("Thiếu thông tin mã hóa E2EE");
        }

        // 3. Hash password
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 4. Decode Base64 (vì DB cột enc_private_key là BLOB)
        byte[] encPrivateKeyBytes;
        try {
            encPrivateKeyBytes = Base64.getDecoder().decode(request.getEncPrivateKey());
        } catch (IllegalArgumentException e) {
             throw new IllegalArgumentException("Định dạng encPrivateKey (Base64) không hợp lệ");
        }

        // 5. Tạo đối tượng User entity
        User user = new User();
        user.setUsername(request.getUsername());
        user.setSdt(request.getSdt());
        user.setPassword(hashedPassword); // Lưu password đã hash
        user.setAvatar(request.getAvatar());
        
        // 6. ✅ LƯU E2EE FIELDS (Đây là phần bị thiếu)
        user.setPublicKey(request.getPublicKey());
        user.setEncPrivateKey(encPrivateKeyBytes); // Lưu dạng byte[]
        user.setSalt(request.getSalt());
        user.setIv(request.getIv());

        // 7. Lưu vào DB
        return userRepository.save(user);
    }

    // (Các hàm getAllUsers, getUserById, updateUser, deleteUser giữ nguyên)
    public List<User> getAllUsers() {
        return userRepository.findAllActiveUsers();
    }
    public User getUserById(int id) {
        return userRepository.findActiveUserById(id);
    }
    public User updateUser(int id, User updatedUser) {
        return userRepository.findById(id).map(user -> {
            user.setUsername(updatedUser.getUsername());
            user.setSdt(updatedUser.getSdt());
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }
            user.setAvatar(updatedUser.getAvatar());
            user.setIsSuspended(updatedUser.getIsSuspended());
            return userRepository.save(user);
        }).orElse(null);
    }
    public boolean deleteUser(int id) {
        return userRepository.findById(id).map(user -> {
            user.setDeletedAt(LocalDateTime.now());
            userRepository.save(user);
            return true;
        }).orElse(false);
    }
}