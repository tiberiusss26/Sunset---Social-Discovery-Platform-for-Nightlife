package com.nightout.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNightRequest {

    @NotBlank @Size(max = 100)  private String title;
    @Size(max = 2000)           private String description;
    @NotNull @FutureOrPresent   private LocalDate date;
    @NotNull                    private LocalTime startTime;
    private LocalTime endTime;
    private boolean specialGuest;
    @Size(max = 100)            private String guestName;
    @Size(max = 100)            private String theme;
    private String flyerImageUrl;
    @Min(0)                     private Integer tableCapacity;
    private List<@Size(max = 50) String> tags;
}