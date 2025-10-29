package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model để hứng dữ liệu trả về từ API lấy danh sách lời mời kết bạn.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FriendRequest {
    
    // API trả về (snake_case)
    @JsonProperty("sender_id")
    private int senderId;
    
    private String username;
    private String avatar;
    private String status; // Sẽ luôn là "pending"

    // Getters & Setters
    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    /**
     * Chuyển đổi FriendRequest (chỉ có senderId) thành một User object
     * để UserListRenderer có thể hiển thị.
     */
    public User toUser() {
        User user = new User();
        user.setId(this.senderId);
        user.setUsername(this.username);
        user.setAvatar(this.avatar);
        user.setSdt("Đã gửi lời mời"); // Dùng SĐT để hiển thị trạng thái
        return user;
    }
}