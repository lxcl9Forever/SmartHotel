package com.smarthotel.service;

import com.smarthotel.dto.auth.CaptchaResponse;
import com.smarthotel.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaptchaService {
    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final StringRedisTemplate redis;
    private final AuthAttemptLimiter limiter;

    public CaptchaResponse create(String ip) {
        limiter.checkCaptchaIssue(ip == null ? "unknown" : ip);
        StringBuilder code = new StringBuilder(5);
        for (int i = 0; i < 5; i++) code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        String id = UUID.randomUUID().toString();
        redis.opsForValue().set("auth:captcha:" + id, code.toString(), Duration.ofMinutes(2));
        return new CaptchaResponse(id, renderPng(code.toString()));
    }

    public void verify(String id, String submitted) {
        if (id == null || !id.matches("^[0-9a-f-]{36}$") || submitted == null) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
        String expected = redis.opsForValue().getAndDelete("auth:captcha:" + id);
        if (expected == null || !expected.equalsIgnoreCase(submitted.trim())) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
    }

    private String renderPng(String code) {
        BufferedImage image = new BufferedImage(150, 48, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(243, 246, 252));
            graphics.fillRect(0, 0, 150, 48);
            for (int i = 0; i < 7; i++) {
                graphics.setColor(new Color(160 + RANDOM.nextInt(55), 170 + RANDOM.nextInt(45), 190 + RANDOM.nextInt(35)));
                graphics.drawLine(RANDOM.nextInt(150), RANDOM.nextInt(48), RANDOM.nextInt(150), RANDOM.nextInt(48));
            }
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
            for (int i = 0; i < code.length(); i++) {
                Graphics2D letter = (Graphics2D) graphics.create();
                int x = 10 + i * 27;
                int y = 33 + RANDOM.nextInt(7) - 3;
                letter.rotate(Math.toRadians(RANDOM.nextInt(25) - 12), x + 10, y - 10);
                letter.setColor(new Color(25 + RANDOM.nextInt(45), 50 + RANDOM.nextInt(50), 100 + RANDOM.nextInt(55)));
                letter.drawString(String.valueOf(code.charAt(i)), x, y);
                letter.dispose();
            }
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("验证码图片生成失败", e);
        }
    }
}
