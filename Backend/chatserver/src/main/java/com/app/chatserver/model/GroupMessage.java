package com.app.chatserver.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_messages")
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "media_url")
    private String mediaUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content")
    private String content;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public enum MessageType {
        TEXT, IMAGE, FILE
    }

    public GroupMessage() {}

    public GroupMessage(Group group, User sender, MessageType messageType, String content, String mediaUrl, String fileName) {
        this.group = group;
        this.sender = sender;
        this.messageType = messageType;
        this.content = content;
        this.mediaUrl = mediaUrl;
        this.fileName = fileName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}