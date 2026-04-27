package com.nightout.repository;

import com.nightout.domain.Night;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface NightRepository extends JpaRepository<Night, UUID> {

    Page<Night> findByVenueIdOrderByDateDesc(UUID venueId, Pageable pageable);

    Page<Night> findByDateAndActiveTrue(LocalDate date, Pageable pageable);

    /**
     * Upcoming nights the user has RSVP'd GOING to.
     * Used for the "My Plans" page.
     */
    @Query("""
            SELECT n FROM Night n
            JOIN n.rsvps r
            WHERE r.user.id = :userId
              AND r.status = 'GOING'
              AND n.date >= :from
            ORDER BY n.date ASC
            """)
    List<Night> findUpcomingNightsForUser(@Param("userId") UUID userId,
                                          @Param("from") LocalDate from);

    /**
     * Nights happening tonight where at least one followed user has RSVP'd GOING.
     * Powers the "Where are my friends going?" social feed.
     */
    @Query("""
            SELECT DISTINCT n FROM Night n
            JOIN n.rsvps r
            JOIN r.user u
            JOIN u.followers f
            WHERE f.id = :userId
              AND n.date = :date
              AND r.status = 'GOING'
            ORDER BY n.date ASC
            """)
    List<Night> findFriendsNightsForDate(@Param("userId") UUID userId,
                                         @Param("date") LocalDate date);
}