package com.app.chatserver.service;

import com.app.chatserver.model.User;
import com.app.chatserver.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Tạo user mới
    public User createUser(User user) {
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    // Lấy tất cả user
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Lấy user theo ID
    public User getUserById(int id) {
        Optional<User> userOpt = userRepository.findById(id);
        return userOpt.orElse(null);
    }

    // Cập nhật user
    public User updateUser(int id, User updatedUser) {
        return userRepository.findById(id).map(user -> {
            user.setUsername(updatedUser.getUsername());
            user.setSdt(updatedUser.getSdt());
            user.setPassword(updatedUser.getPassword());
            user.setAvatar(updatedUser.getAvatar());
            user.setUpdatedAt(LocalDateTime.now());
            user.setIsSuspended(updatedUser.getIsSuspended());
            return userRepository.save(user);
        }).orElse(null);
    }

    // Xóa user (gán deletedAt)
    public boolean deleteUser(int id) {
        return userRepository.findById(id).map(user -> {
            user.setDeletedAt(LocalDateTime.now());
            userRepository.save(user);
            return true;
        }).orElse(false);
    }
}
