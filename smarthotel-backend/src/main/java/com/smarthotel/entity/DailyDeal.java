package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_deal")
public class DailyDeal {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long roomTypeId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer quota;
    private BigDecimal discountRate;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
