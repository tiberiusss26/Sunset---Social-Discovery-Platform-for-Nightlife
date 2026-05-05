package com.nightout.controller;

import com.nightout.dto.*;
import com.nightout.service.NightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class NightController {

    private final NightService nightService;

    @GetMapping("/api/nights/tonight")
    public ResponseEntity<PageResponse<NightSummary>> getTonightsNights(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(nightService.getTonightsNights(
                PageRequest.of(page, size, Sort.by("startTime").ascending())));
    }

    @GetMapping("/api/nights/friends-feed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NightSummary>> getFriendsFeed(Authentication auth) {
        return ResponseEntity.ok(nightService.getFriendsFeed(userId(auth)));
    }

    @GetMapping("/api/nights/my-plans")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NightSummary>> getMyPlans(Authentication auth) {
        return ResponseEntity.ok(nightService.getMyUpcomingNights(userId(auth)));
    }

    @GetMapping("/api/nights/{nightId}")
    public ResponseEntity<NightResponse> getNight(
            @PathVariable UUID nightId, Authentication auth) {
        UUID userId = auth != null ? userId(auth) : null;
        return ResponseEntity.ok(nightService.getNightById(nightId, userId));
    }

    @GetMapping("/api/venues/{venueId}/nights")
    public ResponseEntity<PageResponse<NightSummary>> getNightsForVenue(
            @PathVariable UUID venueId,
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "10")   int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(nightService.getNightsForVenue(venueId, PageRequest.of(page, size, sort)));
    }

    @PostMapping("/api/venues/{venueId}/nights")
    @PreAuthorize("hasAnyRole('VENUE_OWNER', 'ADMIN')")
    public ResponseEntity<NightResponse> createNight(
            @PathVariable UUID venueId,
            @Valid @RequestBody CreateNightRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(nightService.createNight(venueId, request, userId(auth)));
    }

    @PutMapping("/api/nights/{nightId}")
    @PreAuthorize("hasAnyRole('VENUE_OWNER', 'ADMIN')")
    public ResponseEntity<NightResponse> updateNight(
            @PathVariable UUID nightId,
            @Valid @RequestBody CreateNightRequest request,
            Authentication auth) {
        return ResponseEntity.ok(nightService.updateNight(nightId, request, userId(auth)));
    }

    @DeleteMapping("/api/nights/{nightId}")
    @PreAuthorize("hasAnyRole('VENUE_OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteNight(@PathVariable UUID nightId, Authentication auth) {
        nightService.deleteNight(nightId, userId(auth));
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Authentication auth) { return (UUID) auth.getPrincipal(); }
}