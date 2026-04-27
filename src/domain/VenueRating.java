package com.nightout.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

/**
 * A rating (1–5 stars) left by a user for a venue.
 *
 * Business rule: one user can rate one venue only once
 * (enforced by the unique constraint below).
 *
 * When a new rating is saved, VenueService calls venue.recalculateRating()
 * to update the cached averageRating on the Venue entity.
 */
@Entity
@Table(name = "venue_ratings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_rating_user_venue", columnNames = {"user_id", "venue_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueRating extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    /**
     * Rating from 1 to 5. Bean Validation enforces the range at the
     * controller/service level; the DB constraint is a safety net.
     */
    @Min(1) @Max(5)
    @Column(nullable = false)
    private Integer score;

    @Column(length = 500)
    private String comment;
}