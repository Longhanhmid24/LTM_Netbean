package com.app.chatserver.message.dto;

import lombok.Data;
import java.util.Map;

/**
 * DTO (Data Transfer Object) để nhận tin nhắn E2EE Lớp 2 từ client.
 * Khớp với flow bạn mô tả (ciphertext, iv, và danh sách keys).
 */
@Data
public class MessageReceiveDTO {
    
    private int senderId;
    
    // Dùng cho tin nhắn nhóm
    private Integer groupId; // (Null nếu là 1:1)
    
    // Dùng cho tin nhắn 1:1
    private Integer receiverId; // (Null nếu là nhóm)

    // E2EE Data (AES-GCM)
    private String contentCipher; // Nội dung đã mã hóa AES (Base64)
    private String contentIv;     // IV dùng cho AES (Base64)
    
    // E2EE Keys (RSA)
    // Map<RecipientID_String, EncryptedAESKey_String>
    // VD: { "1": "abc...", "2": "xyz..." }
    private Map<String, String> keys; 
    
    private String messageType; // text, image, file...
}