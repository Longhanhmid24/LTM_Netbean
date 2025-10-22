package com.app.chatserver.Repository;


import com.app.chatserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.isSuspended = FALSE")
    List<User> findAllActiveUsers();

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL AND u.isSuspended = FALSE")
    User findActiveUserById(Integer id);
}