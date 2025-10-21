package com.app.chatserver.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_calls")
public class GroupCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false)
    private CallType callType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CallStatus status;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime = LocalDateTime.now();

    @Column(name = "end_time")
    private LocalDateTime endTime;

    public enum CallType {
        AUDIO, VIDEO
    }

    public enum CallStatus {
        ACTIVE, ENDED
    }

    public GroupCall() {}

    public GroupCall(Group group, User initiator, CallType callType, CallStatus status) {
        this.group = group;
        this.initiator = initiator;
        this.callType = callType;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }

    public User getInitiator() { return initiator; }
    public void setInitiator(User initiator) { this.initiator = initiator; }

    public CallType getCallType() { return callType; }
    public void setCallType(CallType callType) { this.callType = callType; }

    public CallStatus getStatus() { return status; }
    public void setStatus(CallStatus status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}