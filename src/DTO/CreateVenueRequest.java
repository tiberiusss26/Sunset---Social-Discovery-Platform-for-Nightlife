package com.nightout.dto;

import com.nightout.domain.Venue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVenueRequest {

    @NotBlank @Size(max = 100) private String name;
    @NotNull                   private Venue.VenueType type;
    @Size(max = 1000)          private String description;
    private String coverImageUrl;
    @NotNull                   private AddressRequest address;
}