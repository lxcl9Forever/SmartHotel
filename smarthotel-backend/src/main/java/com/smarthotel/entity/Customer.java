package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("customer")
public class Customer {

    @TableId(value = "customer_id", type = IdType.AUTO)
    private Long customerId;

    private Long userId;

    private String customerName;

    private String phone;

    private String idType;

    private String idNumber;

    private String gender;

    private LocalDate birthday;

    private Long levelId;

    private String status;

    private String registerSource;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}