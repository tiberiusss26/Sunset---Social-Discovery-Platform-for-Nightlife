package com.nightout.dto;

import com.nightout.domain.Rsvp;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRsvpRequest {

    @NotNull
    private Rsvp.RsvpStatus status;

    @Min(1) @Max(20)
    private Integer tableSize;
}