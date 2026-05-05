package com.nightout.repository;

import com.nightout.domain.VenueRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VenueRatingRepository extends JpaRepository<VenueRating, UUID> {

    Optional<VenueRating> findByUserIdAndVenueId(UUID userId, UUID venueId);

    boolean existsByUserIdAndVenueId(UUID userId, UUID venueId);

    Page<VenueRating> findByVenueId(UUID venueId, Pageable pageable);

    /**
     * Recalculates the true average directly from raw rating rows.
     * Used to verify or correct the cached averageRating field on Venue.
     */
    @Query("SELECT AVG(r.score) FROM VenueRating r WHERE r.venue.id = :venueId")
    Double calculateAverageRating(@Param("venueId") UUID venueId);
}