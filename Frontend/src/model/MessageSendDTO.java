package model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Map;

/**
 * DTO (Data Transfer Object) phía Client
 * Dùng để gửi tin nhắn E2EE Lớp 2 lên server.
 * (Khớp với MessageReceiveDTO của Backend)
 */
public class MessageSendDTO {
    
    private int senderId;
    private Integer groupId; // Null nếu là 1:1
    private Integer receiverId; // Null nếu là nhóm
    
    // E2EE Data (AES-GCM)
    private String contentCipher; // Nội dung đã mã hóa AES (Base64)
    private String contentIv;     // IV dùng cho AES (Base64)
    
    // E2EE Keys (RSA)
    // Map<RecipientID_String, EncryptedAESKey_String>
    private Map<String, String> keys; 
    
    private String messageType; // text, image, file...

    // --- Mapper để chuyển DTO này thành JSON string ---
    private static final ObjectMapper SEND_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public String toJson() {
        try {
            return SEND_MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            System.err.println("Lỗi tạo JSON từ MessageSendDTO: " + e.getMessage());
            return "{}";
        }
    }
    
    // --- Getters & Setters ---
    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    public Integer getGroupId() { return groupId; }
    public void setGroupId(Integer groupId) { this.groupId = groupId; }
    public Integer getReceiverId() { return receiverId; }
    public void setReceiverId(Integer receiverId) { this.receiverId = receiverId; }
    public String getContentCipher() { return contentCipher; }
    public void setContentCipher(String contentCipher) { this.contentCipher = contentCipher; }
    public String getContentIv() { return contentIv; }
    public void setContentIv(String contentIv) { this.contentIv = contentIv; }
    public Map<String, String> getKeys() { return keys; }
    public void setKeys(Map<String, String> keys) { this.keys = keys; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
}