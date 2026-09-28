package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment")
public class Payment {

    @TableId(value = "payment_id", type = IdType.AUTO)
    private Long paymentId;

    private String paymentNo;

    private Long orderId;

    private String paymentMethod;

    private BigDecimal amount;

    private String paymentStatus;

    private String thirdPartyNo;

    private LocalDateTime paidTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}