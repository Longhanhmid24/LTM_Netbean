package com.app.chatserver.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "private_calls")
public class PrivateCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "caller_id", nullable = false)
    private User caller;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

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
        MISSED, COMPLETED, CANCELLED, FAILED
    }

    public PrivateCall() {}

    public PrivateCall(User caller, User receiver, CallType callType, CallStatus status) {
        this.caller = caller;
        this.receiver = receiver;
        this.callType = callType;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getCaller() { return caller; }
    public void setCaller(User caller) { this.caller = caller; }

    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }

    public CallType getCallType() { return callType; }
    public void setCallType(CallType callType) { this.callType = callType; }

    public CallStatus getStatus() { return status; }
    public void setStatus(CallStatus status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}