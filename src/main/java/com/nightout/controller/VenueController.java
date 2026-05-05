package com.nightout.controller;

import com.nightout.dto.*;
import com.nightout.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @GetMapping
    public ResponseEntity<PageResponse<VenueSummary>> getRankedVenues(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "averageRating") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        UUID userId = authentication != null
                ? (UUID) authentication.getPrincipal()
                : UUID.fromString("00000000-0000-0000-0000-000000000000");

        return ResponseEntity.ok(
                venueService.getRankedVenues(userId, LocalDate.now(), PageRequest.of(page, size, sort)));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<VenueSummary>> searchVenues(
            @RequestParam String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(venueService.searchVenues(q, PageRequest.of(page, size)));
    }

    @GetMapping("/{venueId}")
    public ResponseEntity<VenueResponse> getVenue(@PathVariable UUID venueId) {
        return ResponseEntity.ok(venueService.getVenueById(venueId));
    }

    @GetMapping("/{venueId}/ratings")
    public ResponseEntity<PageResponse<RatingResponse>> getVenueRatings(
            @PathVariable UUID venueId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(venueService.getVenueRatings(venueId,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('VENUE_OWNER', 'ADMIN')")
    public ResponseEntity<VenueResponse> createVenue(
            @Valid @RequestBody CreateVenueRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(venueService.createVenue(request, userId(auth)));
    }

    @PutMapping("/{venueId}")
    @PreAuthorize("hasAnyRole('VENUE_OWNER', 'ADMIN')")
    public ResponseEntity<VenueResponse> updateVenue(
            @PathVariable UUID venueId,
            @Valid @RequestBody CreateVenueRequest request,
            Authentication auth) {
        boolean isAdmin = isAdmin(auth);
        return ResponseEntity.ok(venueService.updateVenue(venueId, request, userId(auth), isAdmin));
    }

    @DeleteMapping("/{venueId}")
    @PreAuthorize("hasAnyRole('VENUE_OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteVenue(@PathVariable UUID venueId, Authentication auth) {
        venueService.deleteVenue(venueId, userId(auth), isAdmin(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{venueId}/ratings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RatingResponse> addRating(
            @PathVariable UUID venueId,
            @Valid @RequestBody CreateRatingRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(venueService.addRating(venueId, userId(auth), request));
    }

    private UUID userId(Authentication auth) { return (UUID) auth.getPrincipal(); }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}