package com.tieto.poc.ai_servicenow.messaging;

import com.tieto.poc.ai_servicenow.config.RabbitMQConfig;
import com.tieto.poc.ai_servicenow.dto.OrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes a normal OrderMessage to RabbitMQ.
     */
    public void sendOrder(OrderMessage orderMessage) {

        log.info(
                "[PRODUCER] Publishing orderId={} to exchange={} routingKey={}",
                orderMessage.getOrderId(),
                RabbitMQConfig.ORDERS_EXCHANGE,
                RabbitMQConfig.ORDERS_ROUTING_KEY
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDERS_EXCHANGE,
                RabbitMQConfig.ORDERS_ROUTING_KEY,
                orderMessage
        );

        log.info(
                "[PRODUCER] Order published successfully orderId={}",
                orderMessage.getOrderId()
        );
    }

    /**
     * Sends deliberately malformed JSON.
     *
     * This is used for the poison-message scenario.
     * The normal Jackson converter is intentionally bypassed.
     */
    public void sendPoisonMessage() {

        String malformedJson =
                "{ \"orderId\": \"POISON-001\", \"customerId\": INVALID_JSON }";

        log.error(
                "[PRODUCER] [ERROR_CODE=POISON_MESSAGE] " +
                        "Sending malformed message to RabbitMQ"
        );

        MessageProperties properties =
                new MessageProperties();

        properties.setContentType("application/json");

        Message message = new Message(
                malformedJson.getBytes(StandardCharsets.UTF_8),
                properties
        );

        rabbitTemplate.send(
                RabbitMQConfig.ORDERS_EXCHANGE,
                RabbitMQConfig.ORDERS_ROUTING_KEY,
                message
        );

        log.error(
                "[PRODUCER] Poison message sent successfully"
        );
    }

    /**
     * Publishes an order message with a deliberate
     * consumer-side error scenario.
     */
    public void sendErrorMessage(
            OrderMessage orderMessage,
            String errorType) {

        orderMessage.setSimulateError(true);
        orderMessage.setErrorType(errorType);

        log.warn(
                "[PRODUCER] Sending simulated error message " +
                        "orderId={} errorType={}",
                orderMessage.getOrderId(),
                errorType
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDERS_EXCHANGE,
                RabbitMQConfig.ORDERS_ROUTING_KEY,
                orderMessage
        );
    }
}
