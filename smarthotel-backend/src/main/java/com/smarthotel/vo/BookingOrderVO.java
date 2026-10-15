package com.smarthotel.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingOrderVO {

    private Long orderId;

    private String orderNo;

    private Long roomTypeId;

    private String roomTypeName;

    private Integer roomCount;

    private Integer guestCount;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

    private Integer nights;

    private BigDecimal totalAmount;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private Long promotionId;
    private Long dailyDealId;
    private Long couponId;
    private BigDecimal couponDiscountAmount;

    private String contactName;

    private String contactPhone;

    private String orderStatus;

    private String paymentStatus;
    private String refundStatus;

    private LocalDateTime expireTime;

    private String remark;

    private LocalDateTime createTime;

    private List<BookingGuestVO> guests;
}
