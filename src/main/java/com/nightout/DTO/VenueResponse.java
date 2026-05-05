package com.nightout.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueResponse {

    private UUID id;
    private String name;
    private String type;
    private String description;
    private String coverImageUrl;
    private double averageRating;
    private int totalRatings;
    private boolean verified;
    private AddressResponse address;
    private UserSummary owner;
    private List<NightSummary> upcomingNights;
}