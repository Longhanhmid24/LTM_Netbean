package com.app.chatserver.controller;

import com.app.chatserver.model.PrivateCall;
import com.app.chatserver.service.PrivateCallService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/private-calls")
@CrossOrigin(origins = "*")
public class PrivateCallController {

    private final PrivateCallService privateCallService;

    public PrivateCallController(PrivateCallService privateCallService) {
        this.privateCallService = privateCallService;
    }

    @PostMapping
    public ResponseEntity<PrivateCall> startCall(@RequestParam Integer callerId, @RequestParam Integer receiverId, @RequestParam String callType, @RequestParam String status) {
        PrivateCall.CallType type = PrivateCall.CallType.valueOf(callType.toUpperCase());
        PrivateCall.CallStatus callStatus = PrivateCall.CallStatus.valueOf(status.toUpperCase());
        PrivateCall call = privateCallService.startCall(callerId, receiverId, type, callStatus);
        return call != null ? ResponseEntity.ok(call) : ResponseEntity.badRequest().build();
    }
}