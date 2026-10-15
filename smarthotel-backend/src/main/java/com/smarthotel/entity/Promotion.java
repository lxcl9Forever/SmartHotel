package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("promotion")
public class Promotion {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String name;
    private String type;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal discountRate;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal minAmount;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal reduceAmount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private LocalDateTime createTime;
}
