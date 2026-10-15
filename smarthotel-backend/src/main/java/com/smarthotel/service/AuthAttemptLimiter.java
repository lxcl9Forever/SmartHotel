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
public class AuthAttemptLimiter {
    private final StringRedisTemplate redis;

    public void checkCaptchaIssue(String ip) {
        increment("captcha-ip", ip, 60, Duration.ofMinutes(1));
    }

    public void checkLogin(String ip, String username) {
        increment("login-ip", ip, 100, Duration.ofMinutes(5));
        increment("login-user", username.toLowerCase(java.util.Locale.ROOT), 10, Duration.ofMinutes(15));
    }

    private void increment(String kind, String value, int limit, Duration ttl) {
        String digest = DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
        String key = "auth:attempt:" + kind + ":" + digest;
        Long count = redis.opsForValue().increment(key);
        if (count == null) throw new BusinessException(503, "验证服务暂不可用，请稍后重试");
        if (count == 1) redis.expire(key, ttl);
        if (count > limit) throw new BusinessException(429, "尝试次数过多，请稍后再试");
    }
}
