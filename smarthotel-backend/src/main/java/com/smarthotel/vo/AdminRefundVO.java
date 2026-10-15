package com.smarthotel.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminRefundVO {
    private Long refundId;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String username;
    private String contactName;
    private BigDecimal refundAmount;
    private String refundReason;
    private String refundStatus;
    private LocalDateTime createTime;
    private String refundNo;
    private LocalDateTime refundedTime;
    private String inventorySyncStatus;
}
