package com.smarthotel.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Pattern(regexp = "^1\\d{10}$") String phone,
        @NotBlank @Size(min = 18, max = 18) String idNumber,
        @NotBlank @Size(min = 8, max = 72) String newPassword) {}
