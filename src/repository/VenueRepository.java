package com.nightout.repository;

import com.nightout.domain.Venue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {

    Page<Venue> findByType(Venue.VenueType type, Pageable pageable);

    Page<Venue> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<Venue> findByVerifiedTrue(Pageable pageable);

    @Query("SELECT v FROM Venue v WHERE LOWER(v.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Venue> searchByName(@Param("q") String query, Pageable pageable);

    /**
     * Personalised ranking query.
     * Score = (avgRating * 0.4) + (friendsGoingCount * 0.4) + (totalRsvps * 0.2)
     */
    @Query(value = """
            SELECT v.*,
                   (v.average_rating * 0.4)
                   + (COUNT(DISTINCT CASE WHEN r.user_id IN (:followerIds) THEN r.id END) * 0.4)
                   + (COUNT(DISTINCT r.id) * 0.2) AS score
            FROM venues v
            LEFT JOIN nights n ON n.venue_id = v.id AND n.date = :date AND n.active = true
            LEFT JOIN rsvps r  ON r.night_id = n.id AND r.status = 'GOING'
            WHERE v.verified = true
            GROUP BY v.id
            ORDER BY score DESC
            """,
            countQuery = "SELECT COUNT(*) FROM venues WHERE verified = true",
            nativeQuery = true)
    Page<Venue> findRankedVenues(@Param("date") LocalDate date,
                                 @Param("followerIds") List<UUID> followerIds,
                                 Pageable pageable);
}