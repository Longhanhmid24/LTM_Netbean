package com.app.chatserver.controller;

import com.app.chatserver.model.Friendship;
import com.app.chatserver.service.FriendshipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friendships")
@CrossOrigin(origins = "*")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @PostMapping
    public ResponseEntity<Friendship> createFriendship(@RequestParam Integer userId1, @RequestParam Integer userId2, @RequestParam Integer actionUserId) {
        Friendship friendship = friendshipService.createFriendship(userId1, userId2, actionUserId);
        return friendship != null ? ResponseEntity.ok(friendship) : ResponseEntity.badRequest().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Friendship>> getFriends(@PathVariable Integer userId) {
        return ResponseEntity.ok(friendshipService.getFriends(userId));
    }

    @PutMapping
    public ResponseEntity<Friendship> updateFriendshipStatus(@RequestParam Integer userId1, @RequestParam Integer userId2, @RequestParam String status, @RequestParam Integer actionUserId) {
        Friendship.FriendshipStatus friendshipStatus = Friendship.FriendshipStatus.valueOf(status.toUpperCase());
        Friendship updated = friendshipService.updateFriendshipStatus(userId1, userId2, friendshipStatus, actionUserId);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }
}