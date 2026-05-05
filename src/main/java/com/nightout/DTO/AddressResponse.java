package com.nightout.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private String street;
    private String city;
    private String postalCode;
    private String country;
    private Double latitude;
    private Double longitude;
}