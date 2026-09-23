package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("booking_guest")
public class BookingGuest {

    @TableId(value = "guest_id", type = IdType.AUTO)
    private Long guestId;

    private Long orderId;

    private String guestName;

    private String phone;

    private String idType;

    private String idNumber;

    private Integer isPrimary;

    private LocalDateTime createTime;
}