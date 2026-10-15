package com.smarthotel.dto.promotion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PromotionRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String type;
    private BigDecimal discountRate;
    private BigDecimal minAmount;
    private BigDecimal reduceAmount;
    @NotNull
    private LocalDateTime startTime;
    @NotNull
    private LocalDateTime endTime;
}
