package com.nightout.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a registered user on the platform.
 *
 * Key design decisions explained:
 *
 * 1. We implement Spring Security's UserDetails in the service layer (not here).
 *    Mixing security concerns into your domain entity is an anti-pattern.
 *
 * 2. The self-referencing follow relationship (User follows User) is modelled
 *    as a separate join table "user_follows" with named columns to avoid
 *    Hibernate's default (confusing) column naming.
 *
 * 3. We use Set<> not List<> for collections. List<> with EAGER loading
 *    triggers the "N+1 query problem" — Hibernate fires one query per item.
 *    Set<> with LAZY loading (the default) only loads when you access the field.
 */
@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_username", columnList = "username")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Never store plain text passwords. We store the BCrypt hash.
     * BCrypt output is always 60 chars, but use 255 to be safe.
     */
    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 500)
    private String bio;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    // ── RELATIONSHIPS ─────────────────────────────────────────────────────────

    /**
     * ManyToMany: One user can have multiple roles; one role can belong to
     * many users.
     *
     * @JoinTable defines the join table that Hibernate creates:
     *   CREATE TABLE user_roles (user_id UUID, role_id UUID)
     *
     * CascadeType.MERGE — when you save a User, also merge (update) the
     * attached Role entities if they've changed.
     * fetch = EAGER — always load roles when loading a user. We need this
     * because Spring Security reads roles on every authenticated request.
     */
    @ManyToMany(cascade = {CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * Self-referencing ManyToMany: Users a given user FOLLOWS.
     *
     * The join table "user_follows" has two foreign keys back to "users":
     *   follower_id = the user doing the following
     *   followed_id = the user being followed
     *
     * fetch = LAZY — we DON'T always need to load the entire social graph.
     * Load it only when the service method explicitly accesses this collection.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_follows",
            joinColumns = @JoinColumn(name = "follower_id"),
            inverseJoinColumns = @JoinColumn(name = "followed_id")
    )
    @Builder.Default
    private Set<User> following = new HashSet<>();

    /**
     * The inverse side of the follow relationship.
     * mappedBy = "following" means the "following" field above owns the join table.
     * We use this side to count a user's followers without a separate query.
     */
    @ManyToMany(mappedBy = "following", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> followers = new HashSet<>();

    /**
     * OneToMany: A user can submit many ratings.
     * mappedBy = "user" points to the 'user' field on VenueRating.
     * CascadeType.ALL + orphanRemoval = if a User is deleted, all their
     * ratings are deleted too (no orphaned rows in the database).
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<VenueRating> ratings = new HashSet<>();

    /**
     * OneToMany: A user can make many RSVPs.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Rsvp> rsvps = new HashSet<>();

    // ── HELPER METHODS ────────────────────────────────────────────────────────
    // These keep relationship management consistent — always update BOTH sides.

    public void follow(User other) {
        this.following.add(other);
        other.getFollowers().add(this);
    }

    public void unfollow(User other) {
        this.following.remove(other);
        other.getFollowers().remove(this);
    }

    public void addRole(Role role) {
        this.roles.add(role);
        role.getUsers().add(this);
    }
}