package com.smarthotel.vo;

import com.smarthotel.dto.ai.RoomPreference;

import java.time.LocalDate;
import java.util.List;

public record AIRecommendationVO(RoomPreference parsed, LocalDate checkInDate,
                                 LocalDate checkOutDate, List<RecommendedRoomVO> recommendations,
                                 String reply, BudgetAlternativeVO budgetAlternative) {
}
