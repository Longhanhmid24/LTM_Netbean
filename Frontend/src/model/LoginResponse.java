package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model để hứng dữ liệu trả về khi đăng nhập thành công. (Sử dụng CamelCase vì
 * AuthController trả về Map key là CamelCase)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponse {

    private String message;
    private int userId;
    private String username;
    private String sdt;
    private String avatar;

    // E2EE fields (Lớp 2) - Dùng để tải Private Key
    private String encPrivateKey;
    private String salt;
    private String iv;

    // Getters & Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSdt() {
        return sdt;
    }

    public void setSdt(String sdt) {
        this.sdt = sdt;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    // Getters & Setters cho E2EE fields
    public String getEncPrivateKey() {
        return encPrivateKey;
    }

    public void setEncPrivateKey(String encPrivateKey) {
        this.encPrivateKey = encPrivateKey;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    /**
     * Chuyển đổi dữ liệu login thành đối tượng User hoàn chỉnh.
     */
    public User toUser() {
        User user = new User();
        user.setId(this.userId);
        user.setUsername(this.username);
        user.setSdt(this.sdt);
        user.setAvatar(this.avatar);
        // Không set password vì không cần
        return user;
    }
}
