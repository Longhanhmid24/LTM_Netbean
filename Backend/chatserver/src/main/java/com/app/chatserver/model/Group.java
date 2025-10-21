package com.app.chatserver.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "`groups`")  // Add backticks for reserved word
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Group() {}

    public Group(String name, User creator, String avatar) {
        this.name = name;
        this.creator = creator;
        this.avatar = avatar;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}