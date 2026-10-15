package com.smarthotel.controller;

import com.smarthotel.annotation.OperationLog;
import com.smarthotel.common.result.Result;
import com.smarthotel.dto.auth.LoginRequest;
import com.smarthotel.dto.auth.LoginResponse;
import com.smarthotel.dto.auth.RegisterRequest;
import com.smarthotel.dto.auth.ResetPasswordRequest;
import com.smarthotel.dto.auth.CaptchaResponse;
import com.smarthotel.service.CaptchaService;
import com.smarthotel.service.AuthService;
import com.smarthotel.service.CustomerAccountService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CustomerAccountService customerAccountService;
    private final CaptchaService captchaService;

    @GetMapping("/captcha")
    public Result<CaptchaResponse> captcha(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Result.success(captchaService.create(request.getRemoteAddr()));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        customerAccountService.register(request);
        return Result.success();
    }

    @PostMapping("/login")
    @OperationLog(value = "用户登录", module = "AUTH")
    public Result<LoginResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        return Result.success(
                authService.login(request, httpRequest.getRemoteAddr())
        );
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                      HttpServletRequest httpRequest) {
        customerAccountService.resetPassword(request, httpRequest.getRemoteAddr());
        return Result.success();
    }
}
