package com.app.chatserver.controller;

import com.app.chatserver.model.GroupCall;
import com.app.chatserver.service.GroupCallService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/group-calls")
@CrossOrigin(origins = "*")
public class GroupCallController {

    private final GroupCallService groupCallService;

    public GroupCallController(GroupCallService groupCallService) {
        this.groupCallService = groupCallService;
    }

    @PostMapping
    public ResponseEntity<GroupCall> startCall(@RequestParam Integer groupId, @RequestParam Integer initiatorId, @RequestParam String callType, @RequestParam String status) {
        GroupCall.CallType type = GroupCall.CallType.valueOf(callType.toUpperCase());
        GroupCall.CallStatus callStatus = GroupCall.CallStatus.valueOf(status.toUpperCase());
        GroupCall call = groupCallService.startCall(groupId, initiatorId, type, callStatus);
        return call != null ? ResponseEntity.ok(call) : ResponseEntity.badRequest().build();
    }
}