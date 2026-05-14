package com.nightout.controller;

import com.nightout.dto.CreateRsvpRequest;
import com.nightout.dto.PageResponse;
import com.nightout.dto.RsvpResponse;
import com.nightout.service.RsvpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/nights/{nightId}/rsvps")
@RequiredArgsConstructor
public class RsvpController {

    private final RsvpService rsvpService;

    /**
     * POST /api/nights/{nightId}/rsvps
     *
     * Creates or updates an RSVP for the authenticated user on a night.
     * If the user already has an RSVP for this night, it is updated (upsert).
     *
     * Request body:
     * {
     *   "status": "GOING",
     *   "tableSize": 4
     * }
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RsvpResponse> rsvp(
            @PathVariable UUID nightId,
            @Valid @RequestBody CreateRsvpRequest request,
            Authentication auth) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(rsvpService.createOrUpdateRsvp(nightId, userId(auth), request));
    }

    /**
     * DELETE /api/nights/{nightId}/rsvps
     *
     * Cancels the current user's RSVP for this night.
     * The RSVP row is NOT deleted — its status is set to CANCELLED.
     * This preserves history and allows analytics on cancellation rates.
     */
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelRsvp(
            @PathVariable UUID nightId,
            Authentication auth) {
        rsvpService.cancelRsvp(nightId, userId(auth));
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Authentication auth) {
        return (UUID) auth.getPrincipal();
    }
}