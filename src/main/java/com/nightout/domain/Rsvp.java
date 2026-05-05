package com.nightout.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a user's RSVP for a specific night.
 * This is the join between User and Night, with additional data (status, tableSize).
 *
 * Why not a plain @ManyToMany between User and Night?
 * Because we need extra data on the relationship itself (status, tableSize, etc.)
 * A @ManyToMany join table can only store the two FKs — nothing else.
 * Whenever you need data on a relationship, make it a full entity.
 */
@Entity
@Table(name = "rsvps",
        uniqueConstraints = {
                // A user can only RSVP once per night
                @UniqueConstraint(name = "uq_rsvp_user_night", columnNames = {"user_id", "night_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rsvp extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "night_id", nullable = false)
    private Night night;

    /**
     * GOING     — confirmed attendance
     * INTERESTED — saved for later / maybe
     * CANCELLED  — user cancelled their RSVP
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RsvpStatus status = RsvpStatus.GOING;

    /**
     * How many seats/spots in the party. null = just marking attendance,
     * not reserving a table.
     */
    @Column
    private Integer tableSize;

    public enum RsvpStatus {
        GOING, INTERESTED, CANCELLED
    }
}