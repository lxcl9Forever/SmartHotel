package com.smarthotel.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "smarthotel.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitMQConfig {
    public static final String DELAY_EXCHANGE = "order.delay.exchange";
    public static final String DELAY_QUEUE = "order.delay.queue";
    public static final String TIMEOUT_EXCHANGE = "order.timeout.exchange";
    public static final String TIMEOUT_QUEUE = "order.timeout.queue";
    public static final String DELAY_KEY = "order.delay";
    public static final String TIMEOUT_KEY = "order.timeout";

    @Bean
    DirectExchange orderDelayExchange() {
        return new DirectExchange(DELAY_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange orderTimeoutExchange() {
        return new DirectExchange(TIMEOUT_EXCHANGE, true, false);
    }

    @Bean
    Queue orderDelayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE)
                .deadLetterExchange(TIMEOUT_EXCHANGE)
                .deadLetterRoutingKey(TIMEOUT_KEY)
                .build();
    }

    @Bean
    Queue orderTimeoutQueue() {
        return QueueBuilder.durable(TIMEOUT_QUEUE).build();
    }

    @Bean
    Binding orderDelayBinding(@Qualifier("orderDelayQueue") Queue orderDelayQueue,
                              @Qualifier("orderDelayExchange") DirectExchange orderDelayExchange) {
        return BindingBuilder.bind(orderDelayQueue).to(orderDelayExchange).with(DELAY_KEY);
    }

    @Bean
    Binding orderTimeoutBinding(@Qualifier("orderTimeoutQueue") Queue orderTimeoutQueue,
                                @Qualifier("orderTimeoutExchange") DirectExchange orderTimeoutExchange) {
        return BindingBuilder.bind(orderTimeoutQueue).to(orderTimeoutExchange).with(TIMEOUT_KEY);
    }
}
