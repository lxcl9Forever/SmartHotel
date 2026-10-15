package com.smarthotel.service;

import com.smarthotel.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutProducer {
    private final RabbitTemplate rabbitTemplate;

    @Value("${smarthotel.rabbitmq.enabled:false}")
    private boolean enabled;

    public void sendAfterCommit(Long orderId, LocalDateTime expireTime) {
        if (!enabled) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("订单超时消息必须在事务中登记");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                long remainingMs = Math.max(1_000, Duration.between(LocalDateTime.now(), expireTime).toMillis());
                MessagePostProcessor expiration = message -> {
                    message.getMessageProperties().setExpiration(Long.toString(remainingMs));
                    return message;
                };
                try {
                    rabbitTemplate.convertAndSend(RabbitMQConfig.DELAY_EXCHANGE,
                            RabbitMQConfig.DELAY_KEY, orderId.toString(), expiration);
                } catch (RuntimeException error) {
                    // The existing periodic database scan remains the fallback.
                    log.warn("Order {} timeout message could not be published; scanner will retry", orderId, error);
                }
            }
        });
    }
}
