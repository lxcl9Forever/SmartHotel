package com.smarthotel.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetAlternativeVO(RecommendedRoomVO room, BigDecimal additionalBudget,
                                  LocalDate shorterCheckOutDate, BigDecimal shorterTotalAmount) {
}
