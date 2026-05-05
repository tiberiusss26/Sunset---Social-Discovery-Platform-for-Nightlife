package com.nightout.controller;

import com.nightout.dto.*;
import com.nightout.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getMyProfile(Authentication auth) {
        UUID currentUserId = userId(auth);
        return ResponseEntity.ok(userService.getProfile(currentUserId, currentUserId));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable UUID userId, Authentication auth) {
        return ResponseEntity.ok(userService.getProfile(userId, userId(auth)));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request, Authentication auth) {
        return ResponseEntity.ok(userService.updateProfile(userId(auth), request));
    }

    @PostMapping("/{userId}/follow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> follow(@PathVariable UUID userId, Authentication auth) {
        userService.follow(userId(auth), userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}/follow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unfollow(@PathVariable UUID userId, Authentication auth) {
        userService.unfollow(userId(auth), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<UserSummary>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                userService.searchUsers(q, PageRequest.of(page, size, Sort.by("username"))));
    }

    private UUID userId(Authentication auth) {
        return (UUID) auth.getPrincipal();
    }
}