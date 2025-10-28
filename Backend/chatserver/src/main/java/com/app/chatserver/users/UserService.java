package com.app.chatserver.users;

import com.app.chatserver.model.User;
import com.app.chatserver.Repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

// 🟢 Tạo user mới — hash password + kiểm tra trùng SĐT
public User createUser(User user) {
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());

    // ✅ Kiểm tra trùng SĐT (chỉ kiểm tra số điện thoại)
    boolean phoneExists = userRepository.findAll().stream()
            .anyMatch(u -> u.getSdt().equals(user.getSdt()));

    if (phoneExists) {
        throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
    }

    // ✅ Hash password (nếu có)
    if (user.getPassword() != null && !user.getPassword().isBlank()) {
        String hashed = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashed);
    } else {
        throw new IllegalArgumentException("Mật khẩu không được để trống");
    }

    return userRepository.save(user);
}

    // 🔹 Lấy tất cả user
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 🔹 Lấy user theo ID
    public User getUserById(int id) {
        Optional<User> userOpt = userRepository.findById(id);
        return userOpt.orElse(null);
    }

    // 🔹 Cập nhật user — nếu có password mới thì hash lại
    public User updateUser(int id, User updatedUser) {
        return userRepository.findById(id).map(user -> {
            user.setUsername(updatedUser.getUsername());
            user.setSdt(updatedUser.getSdt());

            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }

            user.setAvatar(updatedUser.getAvatar());
            user.setUpdatedAt(LocalDateTime.now());
            user.setIsSuspended(updatedUser.getIsSuspended());
            return userRepository.save(user);
        }).orElse(null);
    }

    // 🔹 Xóa user (soft delete)
    public boolean deleteUser(int id) {
        return userRepository.findById(id).map(user -> {
            user.setDeletedAt(LocalDateTime.now());
            userRepository.save(user);
            return true;
        }).orElse(false);
    }
}
