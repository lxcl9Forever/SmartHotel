package com.smarthotel.service;

import com.smarthotel.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smarthotel.rabbitmq", name = "enabled", havingValue = "true")
public class OrderTimeoutConsumer {
    private final OrderTimeoutService orderTimeoutService;

    @RabbitListener(queues = RabbitMQConfig.TIMEOUT_QUEUE)
    public void consume(String orderIdText) {
        try {
            orderTimeoutService.closeIfExpired(Long.valueOf(orderIdText));
        } catch (NumberFormatException error) {
            log.warn("Ignoring malformed timeout message: {}", orderIdText);
        } catch (RuntimeException error) {
            // A periodic scan retries database failures without poison-message requeue loops.
            log.error("Timeout processing failed for order {}; scanner will retry", orderIdText, error);
        }
    }
}
