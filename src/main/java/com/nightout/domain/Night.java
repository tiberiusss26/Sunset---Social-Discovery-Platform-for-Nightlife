package com.nightout.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a single night/program posted by a venue owner.
 *
 * A "night" is the central content unit of NightOut. Every bar/club
 * posts a Night for each evening they want to promote, whether it's
 * a regular night or a special themed event with a guest DJ.
 *
 * Relationship summary:
 *   Night ──ManyToOne──▶ Venue  (which venue hosts this night)
 *   Night ──OneToMany──▶ Rsvp   (users who RSVP'd for this night)
 *   Night ──ManyToMany──▶ Tag   (descriptive tags for this night)
 */
@Entity
@Table(name = "nights",
        indexes = {
                @Index(name = "idx_nights_venue_date", columnList = "venue_id, date"),
                @Index(name = "idx_nights_date", columnList = "date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Night extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 2000)
    private String description;

    /**
     * The date of this night. Indexed because almost all queries filter by date.
     */
    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    /**
     * End time can be null for open-ended nights ("until late").
     */
    @Column
    private LocalTime endTime;

    /**
     * Whether this night has a special guest (DJ, artist, etc.)
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean specialGuest = false;

    @Column(length = 100)
    private String guestName;

    @Column(length = 100)
    private String theme;

    @Column(length = 255)
    private String flyerImageUrl;

    /**
     * How many table reservations are available.
     * null = no reservation system for this night.
     */
    @Column
    private Integer tableCapacity;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // ── RELATIONSHIPS ─────────────────────────────────────────────────────────

    /**
     * ManyToOne: Many nights belong to one venue.
     * LAZY loading — we don't always need the full Venue object when loading nights.
     * nullable = false — a night cannot exist without a venue.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    /**
     * OneToMany: One night can receive many RSVPs.
     * CascadeType.ALL + orphanRemoval — deleting a night deletes all its RSVPs.
     */
    @OneToMany(mappedBy = "night", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Rsvp> rsvps = new HashSet<>();

    /**
     * ManyToMany: A night can have multiple tags; a tag can apply to many nights.
     *
     * This side OWNS the join table "night_tags".
     * We don't cascade here — tags are shared global entities. Deleting a Night
     * should NOT delete the tags themselves (other nights use them).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "night_tags",
            joinColumns = @JoinColumn(name = "night_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    // ── HELPERS ───────────────────────────────────────────────────────────────

    public int getRsvpCount() {
        return rsvps.size();
    }

    public boolean hasCapacityFor(int partySize) {
        if (tableCapacity == null) return true; // No limit
        long reserved = rsvps.stream()
                .mapToLong(r -> r.getTableSize() != null ? r.getTableSize() : 1)
                .sum();
        return (reserved + partySize) <= tableCapacity;
    }
}