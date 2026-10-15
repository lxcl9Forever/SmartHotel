package com.smarthotel.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundRecord {

    private Long refundId;

    private Long orderId;

    private Long userId;

    private BigDecimal refundAmount;

    private String refundReason;

    private String refundStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}