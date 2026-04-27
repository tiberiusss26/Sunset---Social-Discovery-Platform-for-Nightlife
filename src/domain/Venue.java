package com.nightout.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a bar, club, restaurant, or any nightlife venue.
 *
 * Relationship summary:
 *   Venue ──OneToOne──▶ Address      (Venue owns the FK: address_id)
 *   Venue ──ManyToOne──▶ User        (the owner; FK: owner_id on this table)
 *   Venue ──OneToMany──▶ Night       (nights hosted at this venue)
 *   Venue ──OneToMany──▶ VenueRating (ratings from users)
 */
@Entity
@Table(name = "venues",
        indexes = {
                @Index(name = "idx_venues_owner", columnList = "owner_id"),
                @Index(name = "idx_venues_type", columnList = "type")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venue extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    /**
     * @Enumerated(EnumType.STRING) — store "BAR", "CLUB" etc. as strings
     * in the database, not integers.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VenueType type;

    @Column(length = 1000)
    private String description;

    @Column(length = 255)
    private String coverImageUrl;

    /**
     * Computed/cached average rating. We update this whenever a new
     * VenueRating is saved, rather than recalculating via AVG() on every
     * request. This is a common denormalization trade-off for read performance.
     */
    @Column(nullable = false)
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalRatings = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    // ── RELATIONSHIPS ─────────────────────────────────────────────────────────

    /**
     * OneToOne: Each venue has exactly one address.
     *
     * @JoinColumn(name = "address_id") — the FK lives on the "venues" table.
     * CascadeType.ALL — saving/deleting a Venue automatically saves/deletes
     * its Address. No need to call addressRepository.save() separately.
     * orphanRemoval = true — if you set venue.setAddress(null), the old
     * Address row is deleted from the database.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    /**
     * ManyToOne: Many venues can be owned by one user.
     * The FK "owner_id" lives on this (venues) table.
     * fetch = LAZY — we don't always need the full owner object.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * OneToMany: A venue hosts many nights over time.
     * mappedBy = "venue" refers to the 'venue' field on Night.
     */
    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Night> nights = new HashSet<>();

    /**
     * OneToMany: A venue receives many ratings from users.
     */
    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<VenueRating> ratings = new HashSet<>();

    // ── ENUM ─────────────────────────────────────────────────────────────────

    public enum VenueType {
        BAR, CLUB, RESTAURANT, LOUNGE, ROOFTOP, OTHER
    }

    // ── HELPER ────────────────────────────────────────────────────────────────

    /**
     * Recalculates the cached averageRating after a new rating is added.
     * Call this from VenueService.addRating() after persisting the new rating.
     */
    public void recalculateRating(double newScore) {
        this.totalRatings++;
        this.averageRating = ((this.averageRating * (this.totalRatings - 1)) + newScore)
                / this.totalRatings;
    }
}