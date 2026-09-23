package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("room_type")
public class RoomType {

    @TableId(value = "room_type_id", type = IdType.AUTO)
    private Long roomTypeId;

    private String typeCode;

    private String typeName;

    private BigDecimal area;

    private String bedType;

    private Integer bedCount;

    private Integer maxGuests;

    private Integer totalRooms;

    private BigDecimal basePrice;

    private String description;

    private Integer status;

    private Integer sortNo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}