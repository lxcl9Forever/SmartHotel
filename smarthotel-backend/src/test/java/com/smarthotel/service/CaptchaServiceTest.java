package com.smarthotel.service;

import com.smarthotel.dto.auth.CaptchaResponse;
import com.smarthotel.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CaptchaServiceTest {
    @Test
    void challengeExpiresAfterOneUse() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        CaptchaService service = new CaptchaService(redis, mock(AuthAttemptLimiter.class));
        CaptchaResponse challenge = service.create("127.0.0.1");
        assertTrue(challenge.image().startsWith("data:image/png;base64,"));
        String key = "auth:captcha:" + challenge.captchaId();
        org.mockito.ArgumentCaptor<String> answer = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(values).set(eq(key), answer.capture(), any());
        when(values.getAndDelete(key)).thenReturn(answer.getValue(), null);

        service.verify(challenge.captchaId(), answer.getValue().toLowerCase());
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.verify(challenge.captchaId(), answer.getValue()));
        assertEquals(400, error.getCode());
    }

    @Test
    void invalidIdentityNumberIsRejected() {
        assertTrue(IdCardValidator.isValid("11010519491231002X"));
        assertFalse(IdCardValidator.isValid("110105194912310021"));
        assertFalse(IdCardValidator.isValid("11010520260230002X"));
    }
}
