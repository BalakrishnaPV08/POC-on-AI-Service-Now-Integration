package com.tieto.poc.ai_servicenow.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDERS_QUEUE =
            "tieto.orders.queue";

    public static final String ORDERS_DLQ =
            "tieto.orders.dlq";

    public static final String AUDIT_QUEUE =
            "tieto.orders.audit.queue";

    public static final String ORDERS_EXCHANGE =
            "tieto.orders.exchange";

    public static final String DEAD_LETTER_EXCHANGE =
            "tieto.orders.dlx";

    public static final String AUDIT_EXCHANGE =
            "tieto.orders.audit.exchange";

    public static final String ORDERS_ROUTING_KEY =
            "order.process";

    public static final String DLQ_ROUTING_KEY =
            "order.dlq";


    // =========================================================
    // Exchanges
    // =========================================================

    @Bean
    public DirectExchange ordersExchange() {
        return new DirectExchange(
                ORDERS_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(
                DEAD_LETTER_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public FanoutExchange auditExchange() {
        return new FanoutExchange(
                AUDIT_EXCHANGE,
                true,
                false
        );
    }


    // =========================================================
    // Queues
    // =========================================================

    @Bean
    public Queue ordersQueue() {
        return QueueBuilder
                .durable(ORDERS_QUEUE)
                .withArgument(
                        "x-dead-letter-exchange",
                        DEAD_LETTER_EXCHANGE
                )
                .withArgument(
                        "x-dead-letter-routing-key",
                        DLQ_ROUTING_KEY
                )
                .withArgument(
                        "x-message-ttl",
                        60000
                )
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable(ORDERS_DLQ)
                .build();
    }

    @Bean
    public Queue auditQueue() {
        return QueueBuilder
                .durable(AUDIT_QUEUE)
                .build();
    }


    // =========================================================
    // Bindings
    // =========================================================

    @Bean
    public Binding ordersBinding(
            Queue ordersQueue,
            DirectExchange ordersExchange) {

        return BindingBuilder
                .bind(ordersQueue)
                .to(ordersExchange)
                .with(ORDERS_ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding(
            Queue deadLetterQueue,
            DirectExchange deadLetterExchange) {

        return BindingBuilder
                .bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding auditBinding(
            Queue auditQueue,
            FanoutExchange auditExchange) {

        return BindingBuilder
                .bind(auditQueue)
                .to(auditExchange);
    }


    // =========================================================
    // JSON Message Converter
    // =========================================================

    @Bean
    public JacksonJsonMessageConverter jacksonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }


    // =========================================================
    // RabbitTemplate
    // =========================================================

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter converter) {

        RabbitTemplate template =
                new RabbitTemplate(connectionFactory);

        template.setMessageConverter(converter);

        return template;
    }
}