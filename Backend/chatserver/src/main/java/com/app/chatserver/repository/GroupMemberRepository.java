package com.app.chatserver.repository;

import com.app.chatserver.model.Group;
import com.app.chatserver.model.GroupMember;
import com.app.chatserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMember.GroupMemberId> {

    @Query("SELECT gm FROM GroupMember gm WHERE gm.group = :group AND gm.member.deletedAt IS NULL AND gm.member.isSuspended = FALSE")
    List<GroupMember> findActiveMembersByGroup(Group group);
}