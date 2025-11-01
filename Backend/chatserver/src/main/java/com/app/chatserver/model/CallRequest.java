package com.app.chatserver.model;

import lombok.Data;

@Data
public class CallRequest {
    private int callerId;
    private int receiverId;
    private String callType; // "audio" hoặc "video"
}
