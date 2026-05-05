package com.nightout.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Represents a physical address for a Venue.
 *
 * This is the "one" side of a OneToOne relationship with Venue.
 * The foreign key (venue_id) lives on the Venue table, not here —
 * that's controlled by @JoinColumn on the Venue side.
 *
 * We also store lat/lng for future map-based search features.
 */
@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends BaseEntity {

    @NotBlank
    @Column(nullable = false, length = 255)
    private String street;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 10)
    private String postalCode;

    @Column(length = 100)
    private String country;

    /**
     * Latitude and longitude for map display and proximity search.
     * Double precision gives ~1cm accuracy — more than enough.
     */
    @Column
    private Double latitude;

    @Column
    private Double longitude;
}