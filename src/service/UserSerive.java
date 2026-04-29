package com.nightout.service;

import com.nightout.domain.User;
import com.nightout.dto.*;
import com.nightout.exception.BusinessRuleException;
import com.nightout.exception.ResourceNotFoundException;
import com.nightout.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId, UUID requestingUserId) {
        User user = findById(userId);
        return mapToProfile(user, userId.equals(requestingUserId));
    }

    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findById(userId);
        if (request.getBio() != null) user.setBio(request.getBio());
        return mapToProfile(userRepository.save(user), true);
    }

    public void follow(UUID followerId, UUID followedId) {
        if (followerId.equals(followedId))
            throw new BusinessRuleException("You cannot follow yourself");
        User follower = findById(followerId);
        User followed = findById(followedId);
        follower.follow(followed);
        userRepository.save(follower);
        log.info("User {} followed user {}", followerId, followedId);
    }

    public void unfollow(UUID followerId, UUID followedId) {
        User follower = findById(followerId);
        User followed = findById(followedId);
        follower.unfollow(followed);
        userRepository.save(follower);
    }

    @Transactional(readOnly = true)
    public List<UUID> getFollowingIds(UUID userId) {
        return findById(userId).getFollowing().stream().map(User::getId).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSummary> searchUsers(String query, Pageable pageable) {
        return PageResponse.from(
                userRepository.searchByUsername(query, pageable)
                        .map(u -> UserSummary.builder()
                                .id(u.getId())
                                .username(u.getUsername())
                                .roles(u.getRoles().stream()
                                        .map(r -> r.getName().name()).toList())
                                .build()));
    }

    private User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private UserProfileResponse mapToProfile(User user, boolean includeEmail) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(includeEmail ? user.getEmail() : null)
                .bio(user.getBio())
                .followingCount(user.getFollowing().size())
                .followersCount(user.getFollowers().size())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).toList())
                .build();
    }
}