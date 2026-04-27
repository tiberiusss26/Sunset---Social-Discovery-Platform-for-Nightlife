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
public class NightSummary {

    private UUID id;
    private String title;
    private LocalDate date;
    private LocalTime startTime;
    private String venueName;
    private UUID venueId;
    private String theme;
    private boolean specialGuest;
    private String guestName;
    private int rsvpCount;
    private int friendsGoingCount;
    private List<String> tags;
}