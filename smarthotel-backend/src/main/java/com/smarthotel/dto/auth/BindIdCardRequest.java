package com.smarthotel.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BindIdCardRequest(
        @NotBlank @Size(min = 18, max = 18) String idNumber,
        @NotBlank String currentPassword) {}
