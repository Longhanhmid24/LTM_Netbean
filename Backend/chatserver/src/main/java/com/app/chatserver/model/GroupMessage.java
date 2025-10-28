package com.app.chatserver.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupMessage {
    private Long id;
    private int groupId;
    private int senderId;
    private String messageType; // text / image / file / video
    private String mediaUrl;
    private String fileName;
    private String content;
    private LocalDateTime timestamp;
}
