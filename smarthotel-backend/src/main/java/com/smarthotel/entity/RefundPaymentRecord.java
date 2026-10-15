package com.smarthotel.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundPaymentRecord {
    private Long refundPaymentId;
    private Long refundId;
    private Long orderId;
    private String refundNo;
    private BigDecimal refundAmount;
    private String refundMethod;
    private String refundStatus;
    private String inventorySyncStatus;
    private LocalDateTime refundedTime;
    private LocalDateTime createTime;
}
