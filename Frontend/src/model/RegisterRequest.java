package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO (Data Transfer Object) để gửi yêu cầu đăng ký
 * chứa cả thông tin user và các khóa E2EE Lớp 2.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterRequest {
    // Thông tin User
    private String username;
    private String sdt;
    private String password;
    private String avatar;

    // --- Thông tin E2EE (Lớp 2) ---
    private String publicKey;
    private String encPrivateKey; // (Base64)
    private String salt;          // (Base64)
    private String iv;            // (Base64)

    // Getters & Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getSdt() { return sdt; }
    public void setSdt(String sdt) { this.sdt = sdt; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    
    // Getters/Setters cho E2EE Lớp 2
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    public String getEncPrivateKey() { return encPrivateKey; }
    public void setEncPrivateKey(String encPrivateKey) { this.encPrivateKey = encPrivateKey; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }
}