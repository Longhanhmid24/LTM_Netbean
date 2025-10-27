package com.app.chatserver.message;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private Long id;
    private int senderId;
    private int receiverId;
    private String content;
    private String messageType; // text / image / file / video
    private String mediaUrl;
    private String fileName;
    private LocalDateTime timestamp;
}
