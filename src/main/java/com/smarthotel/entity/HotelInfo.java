package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("hotel_info")
public class HotelInfo {

    @TableId(value = "hotel_id", type = IdType.AUTO)
    private Long hotelId;

    private String hotelName;

    private String address;

    private String phone;

    private String description;

    private LocalTime checkInTime;

    private LocalTime checkOutTime;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}