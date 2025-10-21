package com.app.chatserver.service;

import com.app.chatserver.model.Group;
import com.app.chatserver.model.User;
import com.app.chatserver.repository.GroupRepository;
import com.app.chatserver.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    public Group createGroup(String name, Integer creatorId, String avatar) {
        User creator = userRepository.findActiveUserById(creatorId);
        if (creator != null) {
            Group group = new Group(name, creator, avatar);
            group.setCreatedAt(LocalDateTime.now());
            return groupRepository.save(group);
        }
        return null;
    }

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public Group getGroupById(Integer id) {
        return groupRepository.findById(id).orElse(null);
    }
}