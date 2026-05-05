package com.nightout.repository;

import com.nightout.domain.Rsvp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RsvpRepository extends JpaRepository<Rsvp, UUID> {

    Optional<Rsvp> findByUserIdAndNightId(UUID userId, UUID nightId);

    boolean existsByUserIdAndNightId(UUID userId, UUID nightId);

    long countByNightIdAndStatus(UUID nightId, Rsvp.RsvpStatus status);

    Page<Rsvp> findByUserId(UUID userId, Pageable pageable);
}