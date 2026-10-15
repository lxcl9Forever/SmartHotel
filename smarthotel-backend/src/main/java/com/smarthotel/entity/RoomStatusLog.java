package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("room_status_log")
public class RoomStatusLog {

    @TableId(type = IdType.AUTO)
    private Long logId;

    private Long roomId;

    private String oldStatus;

    private String newStatus;

    private String changeReason;

    private Long operatorUserId;

    private LocalDateTime changeTime;
}