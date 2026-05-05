package com.nightout.service;

import com.nightout.domain.Night;
import com.nightout.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    /**
     * Sends an RSVP confirmation asynchronously.
     *
     * @Async runs this in Spring's task executor thread pool so the HTTP
     * response returns immediately without waiting for the email.
     * Requires @EnableAsync on NightOutApplication.
     *
     * In the microservices phase this publishes to RabbitMQ instead.
     */
    @Async
    public void sendRsvpConfirmation(User user, Night night) {
        try {
            log.info("[NOTIFICATION] EMAIL → {} | RSVP confirmed for '{}' at {} on {}",
                    user.getEmail(), night.getTitle(),
                    night.getVenue().getName(), night.getDate());
        } catch (Exception e) {
            log.error("Failed to send RSVP email to {}: {}", user.getEmail(), e.getMessage());
        }
    }
}