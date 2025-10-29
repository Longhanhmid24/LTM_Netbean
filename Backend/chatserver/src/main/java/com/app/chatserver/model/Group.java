package com.app.chatserver.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Model Entity cho bảng 'groups'
 */
@Data // Tự động tạo Getters, Setters, toString, v.v.
@Entity
@Table(name = "`groups`") // Dùng dấu `` vì 'groups' là từ khóa SQL
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "creator_id", nullable = false)
    private int creatorId;

    @Column(length = 512)
    private String avatar;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Constructor rỗng (cần cho JPA)
    public Group() {}

    // Constructor (nếu cần)
    public Group(String name, int creatorId, String avatar) {
        this.name = name;
        this.creatorId = creatorId;
        this.avatar = avatar;
    }
}