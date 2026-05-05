package com.nightout.service;

import com.nightout.domain.Rsvp;
import com.nightout.domain.Night;
import com.nightout.domain.User;
import com.nightout.dto.*;
import com.nightout.exception.BusinessRuleException;
import com.nightout.exception.ResourceNotFoundException;
import com.nightout.repository.NightRepository;
import com.nightout.repository.RsvpRepository;
import com.nightout.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RsvpService {

    private final RsvpRepository rsvpRepository;
    private final NightRepository nightRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public RsvpResponse createOrUpdateRsvp(UUID nightId, UUID userId, CreateRsvpRequest req) {
        Night night = nightRepository.findById(nightId)
                .orElseThrow(() -> new ResourceNotFoundException("Night", "id", nightId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (night.getDate().isBefore(LocalDate.now()))
            throw new BusinessRuleException("Cannot RSVP for a past night");

        if (req.getTableSize() != null && !night.hasCapacityFor(req.getTableSize()))
            throw new BusinessRuleException("Not enough table capacity remaining");

        Rsvp rsvp = rsvpRepository.findByUserIdAndNightId(userId, nightId)
                .orElse(Rsvp.builder().user(user).night(night).build());

        rsvp.setStatus(req.getStatus());
        rsvp.setTableSize(req.getTableSize());
        Rsvp saved = rsvpRepository.save(rsvp);

        log.info("RSVP {} for night {} by user {} — {}", saved.getId(), nightId, userId, req.getStatus());

        if (req.getStatus() == Rsvp.RsvpStatus.GOING) {
            notificationService.sendRsvpConfirmation(user, night);
        }

        return mapToResponse(saved);
    }

    public void cancelRsvp(UUID nightId, UUID userId) {
        Rsvp rsvp = rsvpRepository.findByUserIdAndNightId(userId, nightId)
                .orElseThrow(() -> new ResourceNotFoundException("RSVP", "user+night", nightId));
        rsvp.setStatus(Rsvp.RsvpStatus.CANCELLED);
        rsvpRepository.save(rsvp);
        log.info("RSVP cancelled for night {} by user {}", nightId, userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<RsvpResponse> getUserRsvps(UUID userId, Pageable pageable) {
        return PageResponse.from(
                rsvpRepository.findByUserId(userId, pageable).map(this::mapToResponse));
    }

    private RsvpResponse mapToResponse(Rsvp r) {
        return RsvpResponse.builder()
                .id(r.getId())
                .nightId(r.getNight().getId())
                .nightTitle(r.getNight().getTitle())
                .status(r.getStatus().name())
                .tableSize(r.getTableSize())
                .user(UserSummary.builder()
                        .id(r.getUser().getId())
                        .username(r.getUser().getUsername()).build())
                .build();
    }
}