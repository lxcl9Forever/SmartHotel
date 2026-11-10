package com.smarthotel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.common.result.Result;
import com.smarthotel.dto.auth.LoginResponse;
import com.smarthotel.dto.auth.RegisterRequest;
import com.smarthotel.entity.SysUser;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.SysUserMapper;
import com.smarthotel.security.JwtTokenProvider;
import com.smarthotel.service.CustomerAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Available only for account preparation in a disposable local load-test deployment. */
@RestController
@RequestMapping("/api/v1/auth/loadtest")
@RequiredArgsConstructor
@Profile("loadtest")
@ConditionalOnProperty(name = "smarthotel.loadtest.enabled", havingValue = "true")
public class LoadTestAccountController {
    private final CustomerAccountService customerAccountService;
    private final SysUserMapper users;
    private final JwtTokenProvider tokens;

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request,
                                          HttpServletRequest httpRequest) {
        if (!isLoopback(httpRequest.getRemoteAddr())) {
            throw new BusinessException(403, "压测账号只能从本机创建");
        }
        customerAccountService.register(request);
        SysUser user = users.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username().trim()));
        String token = tokens.generateToken(user.getUserId(), user.getUsername(),
                "CUSTOMER", user.getPassword());
        return Result.success(new LoginResponse(token, user.getUserId(), user.getUsername(),
                user.getNickname(), "CUSTOMER"));
    }

    private static boolean isLoopback(String address) {
        if (address == null) return false;
        try {
            return InetAddress.getByName(address).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
