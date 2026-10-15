package com.smarthotel.vo;

public record AdminEmployeeVO(Long employeeId, Long userId, String username, String employeeNo,
                              String employeeName, String phone, String email, String position, String status) {}
