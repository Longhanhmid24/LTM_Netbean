package com.app.chatserver.controller;

import com.app.chatserver.model.Group;
import com.app.chatserver.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<Group> createGroup(@RequestParam String name, @RequestParam Integer creatorId, @RequestParam(required = false) String avatar) {
        Group group = groupService.createGroup(name, creatorId, avatar);
        return group != null ? ResponseEntity.ok(group) : ResponseEntity.badRequest().build();
    }

    @GetMapping
    public ResponseEntity<List<Group>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Group> getGroupById(@PathVariable Integer id) {
        Group group = groupService.getGroupById(id);
        return group != null ? ResponseEntity.ok(group) : ResponseEntity.notFound().build();
    }
}