package com.app.chatserver.repository;

import com.app.chatserver.model.GroupCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupCallRepository extends JpaRepository<GroupCall, Long> {
}