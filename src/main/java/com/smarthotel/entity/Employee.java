package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("employee")
public class Employee {

    @TableId(value = "employee_id", type = IdType.AUTO)
    private Long employeeId;

    private Long userId;

    private String employeeNo;

    private String employeeName;

    private String phone;

    private String email;

    private String position;

    private LocalDate hireDate;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}