package com.app.chatserver.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Friendship {
    private int userId1;
    private int userId2;
    private String status; // pending, accepted, blocked
    private int actionUserId;
}

