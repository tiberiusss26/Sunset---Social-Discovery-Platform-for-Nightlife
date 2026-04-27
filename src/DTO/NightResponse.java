package com.nightout.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NightResponse {

    private UUID id;
    private String title;
    private String description;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean specialGuest;
    private String guestName;
    private String theme;
    private String flyerImageUrl;
    private Integer tableCapacity;
    private int rsvpCount;
    private boolean active;
    private VenueSummary venue;
    private List<String> tags;
    private List<UserSummary> attendees;
}