package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("room_inventory")
public class RoomInventory {

    @TableId(value = "inventory_id", type = IdType.AUTO)
    private Long inventoryId;

    private Long roomTypeId;

    private LocalDate inventoryDate;

    private Integer totalQty;

    private Integer lockedQty;

    private Integer soldQty;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}