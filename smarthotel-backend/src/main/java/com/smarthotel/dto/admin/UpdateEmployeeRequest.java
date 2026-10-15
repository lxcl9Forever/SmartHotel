package com.smarthotel.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEmployeeRequest(
        @NotBlank @Size(max = 50) String employeeName,
        @Size(max = 20) String phone,
        @Size(max = 100) String email,
        @Size(max = 50) String position) {}
