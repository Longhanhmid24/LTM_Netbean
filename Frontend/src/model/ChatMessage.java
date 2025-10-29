package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.ZonedDateTime;
import java.time.LocalDateTime; // Cần cho hàm set (bị xóa)
import java.time.ZoneId; // Cần cho hàm set (bị xóa)

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessage {
    private Long id;
    private int senderId;
    private int receiverId;
    private String messageType;
    private ZonedDateTime timestamp; // Chỉ dùng ZonedDateTime
    
    // --- CÁC TRƯỜNG E2EE (Lớp 2) ---
    private String content; // Sẽ chứa content_cipher (Base64)
    private String contentIv;
    private String encSessionKey; // Khóa AES (đã mã hóa RSA) (Base64)
    
    // --- Các trường plaintext (đã giải mã) ---
    private String decryptedContent;
    private String decryptedMediaUrl;
    private String decryptedFileName;
    
    public ChatMessage() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    public int getReceiverId() { return receiverId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    
    // --- SỬA LỖI ---
    // Chỉ giữ lại MỘT hàm getter và MỘT hàm setter cho ZonedDateTime
    public ZonedDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(ZonedDateTime timestamp) { 
        this.timestamp = timestamp; 
    }
    
    // (XÓA hàm setTimestamp(LocalDateTime ldt) gây xung đột)
    // --- HẾT SỬA LỖI ---

    // Getters/Setters cho E2EE Lớp 2
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContentIv() { return contentIv; }
    public void setContentIv(String contentIv) { this.contentIv = contentIv; }
    public String getEncSessionKey() { return encSessionKey; }
    public void setEncSessionKey(String encSessionKey) { this.encSessionKey = encSessionKey; }

    // Getters/Setters cho nội dung đã giải mã (client-side)
    public String getDecryptedContent() { return decryptedContent; }
    public void setDecryptedContent(String decryptedContent) { this.decryptedContent = decryptedContent; }
    public String getDecryptedMediaUrl() { return decryptedMediaUrl; }
    public void setDecryptedMediaUrl(String decryptedMediaUrl) { this.decryptedMediaUrl = decryptedMediaUrl; }
    public String getDecryptedFileName() { return decryptedFileName; }
    public void setDecryptedFileName(String decryptedFileName) { this.decryptedFileName = decryptedFileName; }
}