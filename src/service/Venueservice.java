package com.nightout.service;

import com.nightout.domain.*;
import com.nightout.dto.*;
import com.nightout.exception.*;
import com.nightout.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * VenueService — all business logic for venue management and discovery.
 *
 * Key concepts demonstrated:
 *  1. @Cacheable / @CacheEvict  — Redis caching to avoid re-running the
 *     expensive ranking query on every request.
 *  2. @Transactional(readOnly=true) — performance hint for read-only paths.
 *  3. Ownership checks at service layer — fine-grained authorisation beyond roles.
 *  4. Entity → DTO mapping — domain objects never leave the service as entities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final VenueRatingRepository ratingRepository;
    private final RsvpRepository rsvpRepository;

    // ── CREATE ────────────────────────────────────────────────────────────────

    /**
     * Creates a new venue owned by the authenticated user.
     *
     * CascadeType.ALL on Venue.address means a single venueRepository.save()
     * inserts BOTH the venue row AND the address row automatically.
     * No need for a separate addressRepository.save().
     */
    @CacheEvict(value = "rankedVenues", allEntries = true)
    public VenueResponse createVenue(CreateVenueRequest req, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));

        Address address = Address.builder()
                .street(req.getAddress().getStreet())
                .city(req.getAddress().getCity())
                .postalCode(req.getAddress().getPostalCode())
                .country(req.getAddress().getCountry())
                .latitude(req.getAddress().getLatitude())
                .longitude(req.getAddress().getLongitude())
                .build();

        Venue venue = Venue.builder()
                .name(req.getName())
                .type(req.getType())
                .description(req.getDescription())
                .coverImageUrl(req.getCoverImageUrl())
                .address(address)
                .owner(owner)
                .build();

        Venue saved = venueRepository.save(venue);
        log.info("Venue created: '{}' by owner {}", saved.getName(), ownerId);
        return mapToResponse(saved, List.of(), 0);
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public VenueResponse getVenueById(UUID venueId) {
        Venue venue = findVenueById(venueId);
        return mapToResponse(venue, venue.getNights().stream().limit(5).toList(), 0);
    }

    /**
     * Personalised ranked feed.
     *
     * @Cacheable("rankedVenues") — Spring checks Redis BEFORE executing the method body.
     *   Cache HIT  → return stored value immediately (no DB round-trip).
     *   Cache MISS → run the method, store the result in Redis, return it.
     *
     * The cache key encodes userId + date + page so each user/day/page
     * combination has its own cache entry.
     */
    @Cacheable(value = "rankedVenues",
            key   = "#userId + '_' + #date + '_' + #pageable.pageNumber")
    @Transactional(readOnly = true)
    public PageResponse<VenueSummary> getRankedVenues(UUID userId,
                                                      LocalDate date,
                                                      Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<UUID> followingIds = user.getFollowing().stream()
                .map(User::getId).toList();

        // The native SQL query needs at least one ID — use a dummy UUID when empty
        List<UUID> ids = followingIds.isEmpty()
                ? List.of(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                : followingIds;

        Page<VenueSummary> page = venueRepository
                .findRankedVenues(date, ids, pageable)
                .map(v -> mapToSummary(v, 0));

        log.debug("Ranked feed: {} results for user {} on {}", page.getTotalElements(), userId, date);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<VenueSummary> searchVenues(String query, Pageable pageable) {
        return PageResponse.from(
                venueRepository.searchByName(query, pageable).map(v -> mapToSummary(v, 0)));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /**
     * @CacheEvict(allEntries=true) — after a venue is updated its ranking may
     * change, so we wipe the entire rankedVenues cache rather than trying to
     * surgically remove individual keys.
     */
    @CacheEvict(value = "rankedVenues", allEntries = true)
    public VenueResponse updateVenue(UUID venueId, CreateVenueRequest req,
                                     UUID requestingUserId, boolean isAdmin) {
        Venue venue = findVenueById(venueId);
        checkOwnership(venue, requestingUserId, isAdmin);

        venue.setName(req.getName());
        venue.setType(req.getType());
        venue.setDescription(req.getDescription());
        venue.setCoverImageUrl(req.getCoverImageUrl());

        // Update address in-place — no need to create a new Address entity
        Address addr = venue.getAddress();
        addr.setStreet(req.getAddress().getStreet());
        addr.setCity(req.getAddress().getCity());
        addr.setPostalCode(req.getAddress().getPostalCode());
        addr.setLatitude(req.getAddress().getLatitude());
        addr.setLongitude(req.getAddress().getLongitude());

        return mapToResponse(venueRepository.save(venue), List.of(), 0);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @CacheEvict(value = "rankedVenues", allEntries = true)
    public void deleteVenue(UUID venueId, UUID requestingUserId, boolean isAdmin) {
        Venue venue = findVenueById(venueId);
        checkOwnership(venue, requestingUserId, isAdmin);
        venueRepository.delete(venue);
        log.info("Venue {} deleted by user {}", venueId, requestingUserId);
    }

    // ── RATINGS ───────────────────────────────────────────────────────────────

    /**
     * Upsert pattern: if the user already rated this venue, update the existing
     * rating; otherwise create a new one.
     * After saving, recalculate the cached averageRating on Venue.
     */
    @CacheEvict(value = "rankedVenues", allEntries = true)
    public RatingResponse addRating(UUID venueId, UUID userId, CreateRatingRequest req) {
        Venue venue = findVenueById(venueId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        VenueRating rating = ratingRepository
                .findByUserIdAndVenueId(userId, venueId)
                .orElse(VenueRating.builder().user(user).venue(venue).build());

        rating.setScore(req.getScore());
        rating.setComment(req.getComment());
        ratingRepository.save(rating);

        venue.recalculateRating(req.getScore());
        venueRepository.save(venue);

        log.info("Rating {}/5 for venue {} by user {}", req.getScore(), venueId, userId);
        return mapRating(rating);
    }

    @Transactional(readOnly = true)
    public PageResponse<RatingResponse> getVenueRatings(UUID venueId, Pageable pageable) {
        return PageResponse.from(
                ratingRepository.findByVenueId(venueId, pageable).map(this::mapRating));
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    private Venue findVenueById(UUID id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", id));
    }

    /**
     * Ownership check: the requesting user must either own the venue OR be ADMIN.
     * This is fine-grained authorisation that SecurityConfig cannot express —
     * SecurityConfig only knows about roles, not about which venue belongs to whom.
     */
    private void checkOwnership(Venue venue, UUID requestingUserId, boolean isAdmin) {
        if (!isAdmin && !venue.getOwner().getId().equals(requestingUserId)) {
            throw new UnauthorizedException("You do not own this venue");
        }
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    public VenueSummary mapToSummary(Venue v, int rsvpCountTonight) {
        return VenueSummary.builder()
                .id(v.getId()).name(v.getName()).type(v.getType().name())
                .city(v.getAddress() != null ? v.getAddress().getCity() : null)
                .averageRating(v.getAverageRating()).totalRatings(v.getTotalRatings())
                .verified(v.isVerified()).coverImageUrl(v.getCoverImageUrl())
                .rsvpCountTonight(rsvpCountTonight).build();
    }

    public VenueResponse mapToResponse(Venue v, List<Night> upcoming, int rsvpCount) {
        return VenueResponse.builder()
                .id(v.getId()).name(v.getName()).type(v.getType().name())
                .description(v.getDescription()).coverImageUrl(v.getCoverImageUrl())
                .averageRating(v.getAverageRating()).totalRatings(v.getTotalRatings())
                .verified(v.isVerified())
                .address(v.getAddress() == null ? null : AddressResponse.builder()
                        .street(v.getAddress().getStreet()).city(v.getAddress().getCity())
                        .postalCode(v.getAddress().getPostalCode())
                        .country(v.getAddress().getCountry())
                        .latitude(v.getAddress().getLatitude())
                        .longitude(v.getAddress().getLongitude()).build())
                .owner(UserSummary.builder()
                        .id(v.getOwner().getId())
                        .username(v.getOwner().getUsername()).build())
                .upcomingNights(upcoming.stream().map(n -> NightSummary.builder()
                        .id(n.getId()).title(n.getTitle()).date(n.getDate())
                        .startTime(n.getStartTime()).venueName(v.getName()).venueId(v.getId())
                        .theme(n.getTheme()).specialGuest(n.isSpecialGuest())
                        .guestName(n.getGuestName()).rsvpCount(n.getRsvpCount())
                        .tags(n.getTags().stream().map(Tag::getName).toList()).build()).toList())
                .build();
    }

    private RatingResponse mapRating(VenueRating r) {
        return RatingResponse.builder()
                .id(r.getId()).score(r.getScore()).comment(r.getComment())
                .user(UserSummary.builder()
                        .id(r.getUser().getId()).username(r.getUser().getUsername()).build())
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)
                .build();
    }
}