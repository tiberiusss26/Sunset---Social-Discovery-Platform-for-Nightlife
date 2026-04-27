package com.nightout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @NotBlank @Size(max = 255) private String street;
    @NotBlank @Size(max = 100) private String city;
    @Size(max = 10)            private String postalCode;
    @Size(max = 100)           private String country;
    private Double latitude;
    private Double longitude;
}