package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("member_level")
public class MemberLevel {

    @TableId(value = "level_id", type = IdType.AUTO)
    private Long levelId;

    private String levelCode;

    private String levelName;

    private BigDecimal minConsumption;

    private BigDecimal discountRate;

    private String description;

    private Integer sortNo;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}