package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("room_assignment")
public class RoomAssignment {

    @TableId(type = IdType.AUTO)
    private Long assignmentId;

    private Long orderId;

    private Long roomId;

    private Long assignedBy;

    private LocalDateTime assignTime;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private String assignmentStatus;

    private String changeReason;
}