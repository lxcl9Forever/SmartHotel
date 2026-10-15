package com.smarthotel.service;

import java.time.LocalDateTime;

/** Shared validity policy for issued coupons; automatic promotions do not issue coupons. */
public final class CouponValidity {
    private CouponValidity() {}

    public static LocalDateTime expiresAt(String source, LocalDateTime issuedAt) {
        return switch (source) {
            case "CHECKIN" -> issuedAt.plusDays(1);
            case "ACTIVITY" -> issuedAt.plusDays(7);
            default -> throw new IllegalArgumentException("Unknown coupon source: " + source);
        };
    }
}
