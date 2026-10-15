package com.smarthotel.service;

import com.smarthotel.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class PasswordResetRateLimiter {
    private final StringRedisTemplate redis;

    public void check(String phone, String ip) {
        increment("phone", phone, 5);
        increment("ip", ip, 20);
    }

    private void increment(String kind, String value, int limit) {
        String digest = DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
        String key = "auth:password-reset:" + kind + ":" + digest;
        Long count = redis.opsForValue().increment(key);
        if (count == null) throw new BusinessException(503, "暂时无法找回密码，请稍后重试");
        if (count == 1) redis.expire(key, Duration.ofMinutes(15));
        if (count > limit) throw new BusinessException(429, "尝试次数过多，请15分钟后再试");
    }
}
