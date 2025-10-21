package com.app.chatserver.repository;

import com.app.chatserver.model.PrivateMessage;
import com.app.chatserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {

    @Query("SELECT pm FROM PrivateMessage pm WHERE (pm.sender = :user1 AND pm.receiver = :user2 OR pm.sender = :user2 AND pm.receiver = :user1) AND pm.sender.deletedAt IS NULL AND pm.receiver.deletedAt IS NULL AND pm.sender.isSuspended = FALSE AND pm.receiver.isSuspended = FALSE")
    List<PrivateMessage> findMessagesBetweenUsers(User user1, User user2);
}