package com.app.chatserver.service;

import com.app.chatserver.model.PrivateCall;
import com.app.chatserver.model.User;
import com.app.chatserver.repository.PrivateCallRepository;
import com.app.chatserver.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PrivateCallService {

    private final PrivateCallRepository privateCallRepository;
    private final UserRepository userRepository;

    public PrivateCallService(PrivateCallRepository privateCallRepository, UserRepository userRepository) {
        this.privateCallRepository = privateCallRepository;
        this.userRepository = userRepository;
    }

    public PrivateCall startCall(Integer callerId, Integer receiverId, PrivateCall.CallType callType, PrivateCall.CallStatus status) {
        User caller = userRepository.findActiveUserById(callerId);
        User receiver = userRepository.findActiveUserById(receiverId);
        if (caller != null && receiver != null) {
            PrivateCall call = new PrivateCall(caller, receiver, callType, status);
            call.setStartTime(LocalDateTime.now());
            return privateCallRepository.save(call);
        }
        return null;
    }
}