package com.smarthotel.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CouponValidityTest {
    @Test
    void sourcesKeepDifferentExpiryTimes() {
        LocalDateTime issued = LocalDateTime.of(2026, 10, 15, 9, 30);
        assertEquals(LocalDateTime.of(2026, 10, 16, 9, 30), CouponValidity.expiresAt("CHECKIN", issued));
        assertEquals(LocalDateTime.of(2026, 10, 22, 9, 30), CouponValidity.expiresAt("ACTIVITY", issued));
    }
}
