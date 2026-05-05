package com.nightout.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRatingRequest {

    @NotNull @Min(1) @Max(5)
    private Integer score;

    @Size(max = 500)
    private String comment;
}