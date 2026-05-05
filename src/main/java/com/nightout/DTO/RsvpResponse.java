package com.nightout.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RsvpResponse {

    private UUID id;
    private UUID nightId;
    private String nightTitle;
    private String status;
    private Integer tableSize;
    private UserSummary user;
}