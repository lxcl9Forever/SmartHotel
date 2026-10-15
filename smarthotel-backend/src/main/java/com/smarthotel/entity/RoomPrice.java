package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("room_price")
public class RoomPrice {

    @TableId(value = "price_id", type = IdType.AUTO)
    private Long priceId;

    private Long roomTypeId;

    private LocalDate priceDate;

    private BigDecimal salePrice;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}