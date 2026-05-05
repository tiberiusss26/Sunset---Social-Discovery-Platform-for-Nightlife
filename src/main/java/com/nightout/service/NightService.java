package com.nightout.service;

import com.nightout.domain.*;
import com.nightout.dto.*;
import com.nightout.exception.ResourceNotFoundException;
import com.nightout.exception.UnauthorizedException;
import com.nightout.repository.NightRepository;
import com.nightout.repository.TagRepository;
import com.nightout.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NightService {

    private final NightRepository nightRepository;
    private final VenueRepository venueRepository;
    private final TagRepository tagRepository;

    public NightResponse createNight(UUID venueId, CreateNightRequest req, UUID ownerId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));

        if (!venue.getOwner().getId().equals(ownerId))
            throw new UnauthorizedException("You do not own this venue");

        Night night = Night.builder()
                .venue(venue).title(req.getTitle()).description(req.getDescription())
                .date(req.getDate()).startTime(req.getStartTime()).endTime(req.getEndTime())
                .specialGuest(req.isSpecialGuest()).guestName(req.getGuestName())
                .theme(req.getTheme()).flyerImageUrl(req.getFlyerImageUrl())
                .tableCapacity(req.getTableCapacity()).build();

        if (req.getTags() != null) {
            for (String tagName : req.getTags()) {
                Tag tag = tagRepository.findByNameIgnoreCase(tagName)
                        .orElseGet(() -> tagRepository.save(
                                Tag.builder().name(tagName.toLowerCase()).build()));
                night.getTags().add(tag);
            }
        }

        Night saved = nightRepository.save(night);
        log.info("Night '{}' created for venue {} on {}", saved.getTitle(), venueId, req.getDate());
        return mapToResponse(saved, night.getRsvpCount());
    }

    @Transactional(readOnly = true)
    public NightResponse getNightById(UUID nightId, UUID requestingUserId) {
        Night night = findById(nightId);
        return mapToResponse(night, night.getRsvpCount());
    }

    @Transactional(readOnly = true)
    public PageResponse<NightSummary> getNightsForVenue(UUID venueId, Pageable pageable) {
        return PageResponse.from(
                nightRepository.findByVenueIdOrderByDateDesc(venueId, pageable)
                        .map(n -> mapToSummary(n, 0)));
    }

    @Transactional(readOnly = true)
    public PageResponse<NightSummary> getTonightsNights(Pageable pageable) {
        return PageResponse.from(
                nightRepository.findByDateAndActiveTrue(LocalDate.now(), pageable)
                        .map(n -> mapToSummary(n, 0)));
    }

    @Transactional(readOnly = true)
    public List<NightSummary> getFriendsFeed(UUID userId) {
        return nightRepository.findFriendsNightsForDate(userId, LocalDate.now())
                .stream().map(n -> mapToSummary(n, 0)).toList();
    }

    @Transactional(readOnly = true)
    public List<NightSummary> getMyUpcomingNights(UUID userId) {
        return nightRepository.findUpcomingNightsForUser(userId, LocalDate.now())
                .stream().map(n -> mapToSummary(n, 0)).toList();
    }

    public NightResponse updateNight(UUID nightId, CreateNightRequest req, UUID ownerId) {
        Night night = findById(nightId);
        if (!night.getVenue().getOwner().getId().equals(ownerId))
            throw new UnauthorizedException("You do not own this night");

        night.setTitle(req.getTitle()); night.setDescription(req.getDescription());
        night.setDate(req.getDate()); night.setStartTime(req.getStartTime());
        night.setEndTime(req.getEndTime()); night.setSpecialGuest(req.isSpecialGuest());
        night.setGuestName(req.getGuestName()); night.setTheme(req.getTheme());
        night.setFlyerImageUrl(req.getFlyerImageUrl()); night.setTableCapacity(req.getTableCapacity());

        night.getTags().clear();
        if (req.getTags() != null) {
            for (String tn : req.getTags()) {
                Tag tag = tagRepository.findByNameIgnoreCase(tn)
                        .orElseGet(() -> tagRepository.save(
                                Tag.builder().name(tn.toLowerCase()).build()));
                night.getTags().add(tag);
            }
        }
        return mapToResponse(nightRepository.save(night), night.getRsvpCount());
    }

    public void deleteNight(UUID nightId, UUID ownerId) {
        Night night = findById(nightId);
        if (!night.getVenue().getOwner().getId().equals(ownerId))
            throw new UnauthorizedException("You do not own this night");
        nightRepository.delete(night);
        log.info("Night {} deleted by owner {}", nightId, ownerId);
    }

    Night findById(UUID id) {
        return nightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Night", "id", id));
    }

    NightSummary mapToSummary(Night n, int friendsCount) {
        return NightSummary.builder()
                .id(n.getId()).title(n.getTitle()).date(n.getDate())
                .startTime(n.getStartTime())
                .venueName(n.getVenue() != null ? n.getVenue().getName() : null)
                .venueId(n.getVenue() != null ? n.getVenue().getId() : null)
                .theme(n.getTheme()).specialGuest(n.isSpecialGuest()).guestName(n.getGuestName())
                .rsvpCount(n.getRsvpCount()).friendsGoingCount(friendsCount)
                .tags(n.getTags().stream().map(Tag::getName).toList()).build();
    }

    NightResponse mapToResponse(Night n, int rsvpCount) {
        List<UserSummary> attendees = n.getRsvps().stream()
                .filter(r -> r.getStatus() == Rsvp.RsvpStatus.GOING).limit(20)
                .map(r -> UserSummary.builder()
                        .id(r.getUser().getId()).username(r.getUser().getUsername()).build())
                .toList();

        return NightResponse.builder()
                .id(n.getId()).title(n.getTitle()).description(n.getDescription())
                .date(n.getDate()).startTime(n.getStartTime()).endTime(n.getEndTime())
                .specialGuest(n.isSpecialGuest()).guestName(n.getGuestName())
                .theme(n.getTheme()).flyerImageUrl(n.getFlyerImageUrl())
                .tableCapacity(n.getTableCapacity()).rsvpCount(rsvpCount).active(n.isActive())
                .venue(n.getVenue() != null ? VenueSummary.builder()
                        .id(n.getVenue().getId()).name(n.getVenue().getName())
                        .type(n.getVenue().getType().name())
                        .averageRating(n.getVenue().getAverageRating())
                        .verified(n.getVenue().isVerified()).build() : null)
                .tags(n.getTags().stream().map(Tag::getName).toList())
                .attendees(attendees).build();
    }
}