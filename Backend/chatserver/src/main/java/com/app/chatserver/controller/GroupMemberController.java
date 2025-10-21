package com.app.chatserver.controller;

import com.app.chatserver.model.GroupMember;
import com.app.chatserver.service.GroupMemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/group-members")
@CrossOrigin(origins = "*")
public class GroupMemberController {

    private final GroupMemberService groupMemberService;

    public GroupMemberController(GroupMemberService groupMemberService) {
        this.groupMemberService = groupMemberService;
    }

    @PostMapping
    public ResponseEntity<GroupMember> addMemberToGroup(@RequestParam Integer groupId, @RequestParam Integer memberId, @RequestParam String role) {
        GroupMember.GroupRole groupRole = GroupMember.GroupRole.valueOf(role.toUpperCase());
        GroupMember groupMember = groupMemberService.addMemberToGroup(groupId, memberId, groupRole);
        return groupMember != null ? ResponseEntity.ok(groupMember) : ResponseEntity.badRequest().build();
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<List<GroupMember>> getGroupMembers(@PathVariable Integer groupId) {
        return ResponseEntity.ok(groupMemberService.getGroupMembers(groupId));
    }
}