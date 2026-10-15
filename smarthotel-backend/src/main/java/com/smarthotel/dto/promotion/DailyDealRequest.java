package com.smarthotel.dto.promotion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DailyDealRequest {
    @NotNull private Long roomTypeId;
    @NotNull private LocalDate checkInDate;
    @NotNull private LocalDate checkOutDate;
    @NotNull private LocalDateTime startTime;
    @NotNull private LocalDateTime endTime;
    @NotNull @Min(1) private Integer quota;
    @NotNull private BigDecimal discountRate;
}
