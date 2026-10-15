package com.smarthotel.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 30) String employeeNo,
        @NotBlank @Size(max = 50) String employeeName,
        @Size(max = 20) String phone,
        @Size(max = 100) String email,
        @Size(max = 50) String position) {}
