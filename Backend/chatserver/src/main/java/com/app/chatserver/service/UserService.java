package com.app.chatserver.service;

import com.app.chatserver.model.User;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService {

    private final Map<Integer, User> users = new HashMap<>();
    private int nextId = 1;

    // Tạo user
    public User createUser(User user) {
        user.setId(nextId++);
        users.put(user.getId(), user);
        return user;
    }

    // Lấy tất cả user
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    // Lấy user theo ID
    public User getUserById(int id) {
        return users.get(id);
    }

    // Cập nhật user
    public User updateUser(int id, User updated) {
        User existing = users.get(id);
        if (existing != null) {
            existing.setUsername(updated.getUsername());
            existing.setEmail(updated.getEmail());
            existing.setPassword(updated.getPassword());
            existing.setFullname(updated.getFullname());
            existing.setAvatar(updated.getAvatar());
        }
        return existing;
    }

    // Xóa user
    public boolean deleteUser(int id) {
        return users.remove(id) != null;
    }
}
