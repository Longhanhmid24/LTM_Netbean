package com.app.chatserver.model;

import lombok.Data;

/**
 * DTO (Data Transfer Object) để nhận yêu cầu đăng ký
 * chứa cả thông tin user và các khóa E2EE.
 */
@Data
public class RegisterRequest {
    // Thông tin User
    private String username;
    private String sdt;
    private String password;
    private String avatar;

    // Thông tin E2EE (Lớp 2)
    private String publicKey;
    private String encPrivateKey; // (frontend gửi dạng Base64)
    private String salt;          // (frontend gửi dạng Base64)
    private String iv;            // (frontend gửi dạng Base64)
}