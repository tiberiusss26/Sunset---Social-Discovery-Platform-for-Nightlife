package com.nightout.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID id;
    private String username;
    private String email;
    private String bio;
    private int followingCount;
    private int followersCount;
    private List<String> roles;
}