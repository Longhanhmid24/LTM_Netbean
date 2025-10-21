package com.app.chatserver.repository;

import com.app.chatserver.model.PrivateCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrivateCallRepository extends JpaRepository<PrivateCall, Long> {
}