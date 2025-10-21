package com.app.chatserver.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "friendships")
public class Friendship implements Serializable {

    @EmbeddedId
    private FriendshipId id;

    @ManyToOne
    @MapsId("userId1")
    @JoinColumn(name = "user_id_1")
    private User user1;

    @ManyToOne
    @MapsId("userId2")
    @JoinColumn(name = "user_id_2")
    private User user2;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FriendshipStatus status = FriendshipStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "action_user_id", nullable = false)
    private User actionUser;

    public enum FriendshipStatus {
        PENDING, ACCEPTED, BLOCKED
    }

    public Friendship() {}

    public Friendship(User user1, User user2, FriendshipStatus status, User actionUser) {
        this.id = new FriendshipId(user1.getId(), user2.getId());
        this.user1 = user1;
        this.user2 = user2;
        this.status = status;
        this.actionUser = actionUser;
    }

    public FriendshipId getId() { return id; }
    public void setId(FriendshipId id) { this.id = id; }

    public User getUser1() { return user1; }
    public void setUser1(User user1) { this.user1 = user1; }

    public User getUser2() { return user2; }
    public void setUser2(User user2) { this.user2 = user2; }

    public FriendshipStatus getStatus() { return status; }
    public void setStatus(FriendshipStatus status) { this.status = status; }

    public User getActionUser() { return actionUser; }
    public void setActionUser(User actionUser) { this.actionUser = actionUser; }

    @Embeddable
    public static class FriendshipId implements Serializable {
        @Column(name = "user_id_1")
        private Integer userId1;

        @Column(name = "user_id_2")
        private Integer userId2;

        public FriendshipId() {}

        public FriendshipId(Integer userId1, Integer userId2) {
            this.userId1 = userId1;
            this.userId2 = userId2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FriendshipId that = (FriendshipId) o;
            return Objects.equals(userId1, that.userId1) && Objects.equals(userId2, that.userId2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId1, userId2);
        }
    }
}