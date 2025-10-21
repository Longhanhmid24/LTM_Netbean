package com.app.chatserver.service;

import com.app.chatserver.model.Friendship;
import com.app.chatserver.model.User;
import com.app.chatserver.repository.FriendshipRepository;
import com.app.chatserver.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public FriendshipService(FriendshipRepository friendshipRepository, UserRepository userRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    public Friendship createFriendship(Integer userId1, Integer userId2, Integer actionUserId) {
        User user1 = userRepository.findActiveUserById(Math.min(userId1, userId2));
        User user2 = userRepository.findActiveUserById(Math.max(userId1, userId2));
        User actionUser = userRepository.findActiveUserById(actionUserId);
        if (user1 != null && user2 != null && actionUser != null) {
            Friendship friendship = new Friendship(user1, user2, Friendship.FriendshipStatus.PENDING, actionUser);
            return friendshipRepository.save(friendship);
        }
        return null;
    }

    public List<Friendship> getFriends(Integer userId) {
        User user = userRepository.findActiveUserById(userId);
        return user != null ? friendshipRepository.findAcceptedFriendsByUser(user) : List.of();
    }

    public Friendship updateFriendshipStatus(Integer userId1, Integer userId2, Friendship.FriendshipStatus status, Integer actionUserId) {
        Friendship friendship = friendshipRepository.findById(new Friendship.FriendshipId(Math.min(userId1, userId2), Math.max(userId1, userId2))).orElse(null);
        User actionUser = userRepository.findActiveUserById(actionUserId);
        if (friendship != null && actionUser != null) {
            friendship.setStatus(status);
            friendship.setActionUser(actionUser);
            return friendshipRepository.save(friendship);
        }
        return null;
    }
}