package com.smarthotel.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@TableName("payment")
public class Payment {


    @TableId(
            value = "payment_id",
            type = IdType.AUTO
    )
    private Long paymentId;



    /**
     * 支付流水号
     */
    private String paymentNo;



    /**
     * 订单ID
     */
    private Long orderId;



    /**
     * 支付方式
     */
    private String paymentMethod;



    /**
     * 支付金额
     */
    private BigDecimal amount;



    /**
     * 支付状态
     */
    private String paymentStatus;



    /**
     * 第三方流水号
     */
    private String thirdPartyNo;



    /**
     * 支付时间
     */
    private LocalDateTime paidTime;



    private LocalDateTime createTime;


    private LocalDateTime updateTime;

}