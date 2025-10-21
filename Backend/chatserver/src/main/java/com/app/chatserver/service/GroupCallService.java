package com.app.chatserver.service;

import com.app.chatserver.model.Group;
import com.app.chatserver.model.GroupCall;
import com.app.chatserver.model.User;
import com.app.chatserver.repository.GroupCallRepository;
import com.app.chatserver.repository.GroupRepository;
import com.app.chatserver.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class GroupCallService {

    private final GroupCallRepository groupCallRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupCallService(GroupCallRepository groupCallRepository, GroupRepository groupRepository, UserRepository userRepository) {
        this.groupCallRepository = groupCallRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    public GroupCall startCall(Integer groupId, Integer initiatorId, GroupCall.CallType callType, GroupCall.CallStatus status) {
        Group group = groupRepository.findById(groupId).orElse(null);
        User initiator = userRepository.findActiveUserById(initiatorId);
        if (group != null && initiator != null) {
            GroupCall call = new GroupCall(group, initiator, callType, status);
            call.setStartTime(LocalDateTime.now());
            return groupCallRepository.save(call);
        }
        return null;
    }
}