package com.smarthotel.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminPaymentVO {
    private Long paymentId;
    private String paymentNo;
    private Long orderId;
    private String orderNo;
    private String paymentMethod;
    private BigDecimal amount;
    private String paymentStatus;
    private String thirdPartyNo;
    private LocalDateTime paidTime;
}
