package com.nightout.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueSummary {

    private UUID id;
    private String name;
    private String type;
    private String city;
    private double averageRating;
    private int totalRatings;
    private boolean verified;
    private String coverImageUrl;
    private int rsvpCountTonight;
}