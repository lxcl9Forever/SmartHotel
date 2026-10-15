package com.smarthotel.vo;

import java.math.BigDecimal;

public record RecommendedRoomVO(Long roomTypeId, String roomTypeName,
                                BigDecimal totalAmount, int availableQty,
                                BigDecimal rating, String reason) {
}
