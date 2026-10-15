package com.smarthotel.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AdminOrderVO {

    private Long orderId;

    private String orderNo;

    private Long customerId;

    private Long roomTypeId;

    /**
     * 房型名称
     */
    private String roomTypeName;

    private Integer roomCount;

    private Integer guestCount;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

    private Integer nights;

    private BigDecimal totalAmount;

    private String contactName;

    private String contactPhone;

    private String orderStatus;

    private String paymentStatus;

    private LocalDateTime expireTime;

    private String remark;

    private LocalDateTime createTime;
}