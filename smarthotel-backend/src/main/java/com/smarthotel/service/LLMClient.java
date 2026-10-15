package com.smarthotel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthotel.dto.ai.RoomPreference;
import com.smarthotel.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LLMClient {
    public record Interpretation(RoomPreference preference, String greeting) {
    }

    private final ObjectMapper mapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();

    @Value("${smarthotel.ai.llm-enabled:false}")
    private boolean enabled;
    @Value("${smarthotel.ai.llm-url:}")
    private String url;
    @Value("${smarthotel.ai.llm-model:}")
    private String model;
    @Value("${smarthotel.ai.llm-api-key:}")
    private String apiKey;

    public Interpretation parse(String text) {
        if (!enabled || !StringUtils.hasText(url) || !StringUtils.hasText(model)
                || !StringUtils.hasText(apiKey)) {
            throw new BusinessException(503, "智能选房模型尚未配置，请联系管理员检查 DeepSeek 环境变量");
        }
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "model", model,
                    "response_format", Map.of("type", "json_object"),
                    "max_tokens", 384,
                    "messages", List.of(
                            Map.of("role", "system", "content", "你是酒店的智能选房助手。请从顾客需求中提取 people、days、budget、preference，并在 greeting 中写一至两句正式、礼貌、亲切的回复，感谢顾客说明入住需求，表达愿意帮助挑选房型。只输出 JSON 对象，例如 {\"people\":3,\"days\":3,\"budget\":1000,\"preference\":\"安静\",\"greeting\":\"您好，感谢您分享入住安排与喜好。我会结合您的需求，认真为您挑选合适的房型。\"}。未说明人数时用2，未说明晚数时用1，未知预算用 null，preference 用简短中文。greeting 不得包含价格、房型名称、库存、数字或未经核实的承诺。不要添加解释。"),
                            Map.of("role", "user", "content", text))));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("LLM request returned HTTP {}", response.statusCode());
                throw new BusinessException(502, "智能选房模型暂时无法使用，请稍后重试");
            }
            JsonNode root = mapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank()) {
                throw new BusinessException(502, "智能选房模型未返回有效结果，请重试");
            }
            JsonNode parsed = mapper.readTree(content.asText());
            int people = requiredInt(parsed.path("people"), 1, 20);
            int days = requiredInt(parsed.path("days"), 1, 30);
            BigDecimal budget = parsed.path("budget").isNumber()
                    ? parsed.path("budget").decimalValue() : null;
            if (budget != null && budget.signum() <= 0) budget = null;
            String preference = parsed.path("preference").isTextual()
                    ? parsed.path("preference").asText() : "";
            if (preference.length() > 100) preference = preference.substring(0, 100);
            String greeting = parsed.path("greeting").isTextual()
                    ? parsed.path("greeting").asText().trim() : "";
            if (greeting.isBlank() || greeting.length() > 120
                    || greeting.matches(".*[0-9０-９¥￥].*")) {
                throw new BusinessException(502, "智能选房模型回复无效，请重试");
            }
            return new Interpretation(new RoomPreference(people, days, budget, preference, "LLM"), greeting);
        } catch (BusinessException error) {
            throw error;
        } catch (Exception error) {
            log.warn("LLM parsing unavailable: {}", error.getClass().getSimpleName());
            throw new BusinessException(502, "智能选房模型调用失败，请稍后重试");
        }
    }

    private int requiredInt(JsonNode node, int min, int max) {
        if (!node.isIntegralNumber() || node.asInt() < min || node.asInt() > max) {
            throw new BusinessException(502, "智能选房模型未正确理解入住人数或晚数，请重新描述");
        }
        return node.asInt();
    }
}
