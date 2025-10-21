package com.app.chatserver.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "group_members")
public class GroupMember implements Serializable {

    @EmbeddedId
    private GroupMemberId id;

    @ManyToOne
    @MapsId("groupId")
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne
    @MapsId("memberId")
    @JoinColumn(name = "member_id")
    private User member;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private GroupRole role = GroupRole.MEMBER;

    public enum GroupRole {
        ADMIN, MEMBER
    }

    public GroupMember() {}

    public GroupMember(Group group, User member, GroupRole role) {
        this.id = new GroupMemberId(group.getId(), member.getId());
        this.group = group;
        this.member = member;
        this.role = role;
    }

    public GroupMemberId getId() { return id; }
    public void setId(GroupMemberId id) { this.id = id; }

    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }

    public User getMember() { return member; }
    public void setMember(User member) { this.member = member; }

    public GroupRole getRole() { return role; }
    public void setRole(GroupRole role) { this.role = role; }

    @Embeddable
    public static class GroupMemberId implements Serializable {
        @Column(name = "group_id")
        private Integer groupId;

        @Column(name = "member_id")
        private Integer memberId;

        public GroupMemberId() {}

        public GroupMemberId(Integer groupId, Integer memberId) {
            this.groupId = groupId;
            this.memberId = memberId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GroupMemberId that = (GroupMemberId) o;
            return Objects.equals(groupId, that.groupId) && Objects.equals(memberId, that.memberId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, memberId);
        }
    }
}