package com.nightout.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {

    private UUID id;
    private String username;
    private List<String> roles;
}