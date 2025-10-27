package com.app.chatserver.model;

import lombok.Data;

@Data
public class CallSignal {
    private String type;        // offer / answer / candidate / hangup
    private int callerId;
    private int receiverId;
    private String sdp;         // nếu là offer/answer
    private String candidate;   // nếu là ICE candidate

    // 🟢 Thêm 2 trường cho E2EE
    private boolean encrypted;  // true nếu payload đã mã hóa
    private String payload;     // chứa dữ liệu mã hóa
}
