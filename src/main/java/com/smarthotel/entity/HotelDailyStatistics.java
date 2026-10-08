package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("hotel_daily_statistics")
public class HotelDailyStatistics {

    @TableId(
            value = "stat_id",
            type = IdType.AUTO
    )
    private Long statId;

    private LocalDate statDate;

    private Integer newOrderCount;

    private Integer checkInCount;

    private Integer checkOutCount;

    private Integer occupiedRoomCount;

    private Integer availableRoomCount;

    private BigDecimal occupancyRate;

    private BigDecimal roomRevenue;

    private BigDecimal extraRevenue;

    private BigDecimal refundAmount;

    private BigDecimal totalRevenue;

    private LocalDateTime createTime;
}