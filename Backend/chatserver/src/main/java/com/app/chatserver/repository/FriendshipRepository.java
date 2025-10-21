package com.app.chatserver.repository;

import com.app.chatserver.model.Friendship;
import com.app.chatserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Friendship.FriendshipId> {

    @Query("SELECT f FROM Friendship f WHERE (f.user1 = :user OR f.user2 = :user) AND f.status = 'ACCEPTED' AND f.user1.deletedAt IS NULL AND f.user2.deletedAt IS NULL AND f.user1.isSuspended = FALSE AND f.user2.isSuspended = FALSE")
    List<Friendship> findAcceptedFriendsByUser(User user);
}