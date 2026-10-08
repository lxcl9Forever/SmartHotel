package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("check_out_record")
public class CheckOutRecord {

    @TableId(type = IdType.AUTO)
    private Long checkOutId;

    private Long checkInId;

    private LocalDateTime actualCheckOutTime;

    private BigDecimal roomCharge;

    private BigDecimal extraCharge;

    private BigDecimal refundDeposit;

    private BigDecimal settlementAmount;

    private Long handledBy;

    private String remark;
}