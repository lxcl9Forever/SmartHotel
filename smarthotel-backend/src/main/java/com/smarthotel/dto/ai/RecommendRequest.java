package com.smarthotel.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RecommendRequest {
    @NotBlank
    @Size(max = 500)
    private String text;
    private LocalDate checkInDate;
}
