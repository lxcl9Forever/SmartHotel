package com.smarthotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.dto.auth.LoginRequest;
import com.smarthotel.dto.auth.LoginResponse;
import com.smarthotel.entity.SysRole;
import com.smarthotel.entity.SysUser;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.SysRoleMapper;
import com.smarthotel.mapper.SysUserMapper;
import com.smarthotel.security.JwtTokenProvider;
import com.smarthotel.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;

    private final SysRoleMapper sysRoleMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponse login(LoginRequest request) {

        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(
                                SysUser::getUsername,
                                request.getUsername()
                        )
        );

        if (user == null) {
            throw new BusinessException(
                    401,
                    "用户名或密码错误"
            );
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(
                    403,
                    "账号已被停用"
            );
        }

        boolean passwordCorrect =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        if (!passwordCorrect) {
            throw new BusinessException(
                    401,
                    "用户名或密码错误"
            );
        }

        SysRole role =
                sysRoleMapper.selectById(
                        user.getRoleId()
                );

        if (role == null) {
            throw new BusinessException(
                    500,
                    "用户角色不存在"
            );
        }

        String token =
                jwtTokenProvider.generateToken(
                        user.getUserId(),
                        user.getUsername(),
                        role.getRoleCode()
                );

        return new LoginResponse(
                token,
                user.getUserId(),
                user.getUsername(),
                user.getNickname(),
                role.getRoleCode()
        );
    }
}