package com.app.chatserver.service;

import com.app.chatserver.model.Group;
import com.app.chatserver.model.GroupMember;
import com.app.chatserver.model.User;
import com.app.chatserver.repository.GroupMemberRepository;
import com.app.chatserver.repository.GroupRepository;
import com.app.chatserver.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupMemberService {

    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupMemberService(GroupMemberRepository groupMemberRepository, GroupRepository groupRepository, UserRepository userRepository) {
        this.groupMemberRepository = groupMemberRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    public GroupMember addMemberToGroup(Integer groupId, Integer memberId, GroupMember.GroupRole role) {
        Group group = groupRepository.findById(groupId).orElse(null);
        User member = userRepository.findActiveUserById(memberId);
        if (group != null && member != null) {
            GroupMember groupMember = new GroupMember(group, member, role);
            return groupMemberRepository.save(groupMember);
        }
        return null;
    }

    public List<GroupMember> getGroupMembers(Integer groupId) {
        Group group = groupRepository.findById(groupId).orElse(null);
        return group != null ? groupMemberRepository.findActiveMembersByGroup(group) : List.of();
    }
}