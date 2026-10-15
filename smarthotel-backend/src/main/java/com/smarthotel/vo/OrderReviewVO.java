package com.smarthotel.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrderReviewVO {
    private Long reviewId;
    private Long orderId;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private Integer rating;
    private String content;
    private String status;
    private LocalDateTime createTime;
}
