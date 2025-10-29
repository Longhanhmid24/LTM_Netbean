package com.app.chatserver.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // Không gửi các trường null
public class ChatMessage {
    private Long id;
    private int senderId;
    private int receiverId; // (Sẽ là 0 nếu là tin nhóm)
    
    // CÁC TRƯỜNG E2EE (Lớp 2)
    private String content; // (Chứa content_cipher)
    private String contentIv;
    private String encSessionKey; // Khóa AES đã mã hóa RSA (dành riêng cho người nhận)

    // CÁC TRƯỜNG CŨ (Không dùng trong E2EE Lớp 2)
    // private String mediaUrl;
    // private String fileName;
    
    private String messageType;
    private LocalDateTime timestamp;
}