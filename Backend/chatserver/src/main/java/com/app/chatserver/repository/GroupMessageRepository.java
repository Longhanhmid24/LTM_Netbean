package com.app.chatserver.repository;

import com.app.chatserver.model.Group;
import com.app.chatserver.model.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {

    @Query("SELECT gm FROM GroupMessage gm WHERE gm.group = :group AND gm.sender.deletedAt IS NULL AND gm.sender.isSuspended = FALSE")
    List<GroupMessage> findMessagesByGroup(Group group);
}