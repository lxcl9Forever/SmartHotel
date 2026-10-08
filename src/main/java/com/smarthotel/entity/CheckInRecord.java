package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("check_in_record")
public class CheckInRecord {

    @TableId(type = IdType.AUTO)
    private Long checkInId;

    private Long assignmentId;

    private Long primaryGuestId;

    private LocalDateTime actualCheckInTime;

    private BigDecimal depositAmount;

    private Long handledBy;

    private String remark;

    private LocalDateTime createTime;
}