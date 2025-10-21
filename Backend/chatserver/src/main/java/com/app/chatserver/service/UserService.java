package com.app.chatserver.service;

import com.app.chatserver.model.User;
import com.app.chatserver.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(User user) {
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setIsSuspended(false);
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAllActiveUsers();
    }

    public User getUserById(Integer id) {
        return userRepository.findActiveUserById(id);
    }

    public User updateUser(Integer id, User updated) {
        User existing = userRepository.findActiveUserById(id);
        if (existing != null) {
            existing.setUsername(updated.getUsername());
            existing.setSdt(updated.getSdt());
            existing.setAvatar(updated.getAvatar());
            existing.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(existing);
        }
        return null;
    }

    public boolean deleteUser(Integer id) {
        User user = userRepository.findActiveUserById(id);
        if (user != null) {
            user.setDeletedAt(LocalDateTime.now());
            user.setIsSuspended(true);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public boolean suspendUser(Integer id) {
        User user = userRepository.findActiveUserById(id);
        if (user != null) {
            user.setIsSuspended(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            return true;
        }
        return false;
    }
}