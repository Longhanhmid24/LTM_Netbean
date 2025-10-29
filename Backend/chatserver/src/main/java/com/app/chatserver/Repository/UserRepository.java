package com.app.chatserver.Repository;

import com.app.chatserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // Import Optional

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.isSuspended = FALSE")
    List<User> findAllActiveUsers();

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL AND u.isSuspended = FALSE")
    User findActiveUserById(Integer id);
    
    // ✅ HÀM MỚI: Tìm user bằng username HOẶC sdt (cho đăng nhập)
    @Query("SELECT u FROM User u WHERE (u.username = :loginId OR u.sdt = :loginId) AND u.deletedAt IS NULL AND u.isSuspended = FALSE")
    Optional<User> findActiveUserByUsernameOrSdt(String loginId);
    
    // ✅ HÀM MỚI: Kiểm tra SĐT tồn tại (cho đăng ký)
    boolean existsBySdt(String sdt);
    
    // ✅ HÀM MỚI: Kiểm tra Username tồn tại (cho đăng ký)
    boolean existsByUsername(String username);
}