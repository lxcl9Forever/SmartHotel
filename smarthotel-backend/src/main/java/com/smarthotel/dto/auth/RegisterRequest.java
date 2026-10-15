package com.smarthotel.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Pattern(regexp = "^1\\d{10}$") String phone) {}
