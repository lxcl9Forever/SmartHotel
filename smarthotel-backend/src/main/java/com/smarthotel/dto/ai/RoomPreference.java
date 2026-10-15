package com.smarthotel.dto.ai;

import java.math.BigDecimal;

public record RoomPreference(int people, int days, BigDecimal budget,
                             String preference, String source) {
}
