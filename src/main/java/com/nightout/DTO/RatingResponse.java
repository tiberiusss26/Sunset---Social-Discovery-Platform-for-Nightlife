package com.nightout.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponse {

    private UUID id;
    private int score;
    private String comment;
    private UserSummary user;
    private String createdAt;
}