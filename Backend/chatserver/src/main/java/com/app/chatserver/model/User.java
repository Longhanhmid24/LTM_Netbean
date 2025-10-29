package com.app.chatserver.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String sdt;

    @Column(nullable = false)
    private String password; // Đã hash (Bcrypt)

    private String avatar;

    // --- CỘT MỚI CHO E2EE (LỚP 2) ---
    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey; // Lưu dạng Base64 text

    @Lob
    @Column(name = "enc_private_key", nullable = false, columnDefinition = "BLOB")
    private byte[] encPrivateKey; // Lưu dạng byte array (BLOB)

    @Column(nullable = false)
    private String salt; // Lưu dạng Base64 text

    @Column(nullable = false)
    private String iv; // Lưu dạng Base64 text
    // --- KẾT THÚC CỘT MỚI ---

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Column(name = "is_suspended", nullable = false)
    private Boolean isSuspended = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public User() {}

    // Getters và Setters (Lombok @Data không hoạt động tốt với @Lob byte[])
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getSdt() { return sdt; }
    public void setSdt(String sdt) { this.sdt = sdt; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public byte[] getEncPrivateKey() { return encPrivateKey; }
    public void setEncPrivateKey(byte[] encPrivateKey) { this.encPrivateKey = encPrivateKey; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public Boolean getIsSuspended() { return isSuspended; }
    public void setIsSuspended(Boolean isSuspended) { this.isSuspended = isSuspended; }
}