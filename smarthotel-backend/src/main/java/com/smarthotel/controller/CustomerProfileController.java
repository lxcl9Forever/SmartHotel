package com.smarthotel.controller;

import com.smarthotel.common.result.Result;
import com.smarthotel.dto.auth.ChangePasswordRequest;
import com.smarthotel.dto.auth.BindIdCardRequest;
import com.smarthotel.dto.auth.UpdateProfileRequest;
import com.smarthotel.service.CustomerAccountService;
import com.smarthotel.vo.CustomerProfileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/profile")
@RequiredArgsConstructor
public class CustomerProfileController {
    private final CustomerAccountService service;

    @GetMapping
    public Result<CustomerProfileVO> get(Authentication authentication) {
        return Result.success(service.profile((Long) authentication.getCredentials()));
    }

    @PutMapping
    public Result<CustomerProfileVO> update(Authentication authentication,
                                            @Valid @RequestBody UpdateProfileRequest request) {
        return Result.success(service.updateProfile((Long) authentication.getCredentials(), request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(Authentication authentication,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword((Long) authentication.getCredentials(), request);
        return Result.success();
    }

    @PostMapping("/id-card")
    public Result<Void> bindIdCard(Authentication authentication,
                                   @Valid @RequestBody BindIdCardRequest request) {
        service.bindIdCard((Long) authentication.getCredentials(), request);
        return Result.success();
    }
}
