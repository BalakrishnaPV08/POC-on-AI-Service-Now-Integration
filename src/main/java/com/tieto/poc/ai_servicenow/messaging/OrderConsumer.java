package com.tieto.poc.ai_servicenow.messaging;

import com.tieto.poc.ai_servicenow.config.RabbitMQConfig;
import com.tieto.poc.ai_servicenow.dto.OrderMessage;
import com.tieto.poc.ai_servicenow.model.AuditLog;
import com.tieto.poc.ai_servicenow.model.Order;
import com.tieto.poc.ai_servicenow.repository.AuditLogRepository;
import com.tieto.poc.ai_servicenow.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderRepository orderRepository;
    private final AuditLogRepository auditLogRepository;

    @RabbitListener(
            queues = RabbitMQConfig.ORDERS_QUEUE
    )
    @Transactional
    public void consumeOrder(OrderMessage message) {

        String orderId = message.getOrderId();

        log.info(
                "[CONSUMER] Received orderId={} customerId={}",
                orderId,
                message.getCustomerId()
        );

        Order order = orderRepository
                .findByOrderId(orderId)
                .orElseThrow(() -> {

                    log.error(
                            "[CONSUMER] " +
                                    "[ERROR_CODE=ORDER_NOT_FOUND] " +
                                    "Order not found orderId={}",
                            orderId
                    );

                    return new IllegalStateException(
                            "Order not found: " + orderId
                    );
                });

        // -----------------------------------------------------
        // Mark order as PROCESSING
        // -----------------------------------------------------

        order.setStatus(
                Order.OrderStatus.PROCESSING
        );

        order.setUpdatedAt(
                LocalDateTime.now()
        );

        orderRepository.save(order);

        saveAudit(
                orderId,
                "PROCESS_ORDER",
                "STARTED",
                "Order processing started",
                null
        );

        log.info(
                "[CONSUMER] Order marked PROCESSING orderId={}",
                orderId
        );


        // -----------------------------------------------------
        // Scenario #7 - Consumer Processing Error
        // -----------------------------------------------------

        if (message.isSimulateError()
                && "PROCESSING_ERROR"
                .equalsIgnoreCase(message.getErrorType())) {

            log.error(
                    "[CONSUMER] " +
                            "[ERROR_CODE=CONSUMER_PROCESSING_FAILURE] " +
                            "Simulated processing failure orderId={}",
                    orderId
            );

            saveAudit(
                    orderId,
                    "PROCESS_ORDER",
                    "ERROR",
                    "Simulated consumer processing failure",
                    "CONSUMER_PROCESSING_FAILURE"
            );

            throw new RuntimeException(
                    "Simulated consumer processing failure"
            );
        }


        // -----------------------------------------------------
        // Scenario #9 - Slow Consumer Processing
        // -----------------------------------------------------

        if (message.isSimulateError()
                && "SLOW_PROCESSING"
                .equalsIgnoreCase(message.getErrorType())) {

            log.warn(
                    "[CONSUMER] " +
                            "[ERROR_CODE=SLOW_CONSUMER_PROCESSING] " +
                            "Simulating slow processing orderId={}",
                    orderId
            );

            try {

                Thread.sleep(7000);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                log.error(
                        "[CONSUMER] Consumer interrupted orderId={}",
                        orderId,
                        e
                );

                throw new RuntimeException(
                        "Consumer interrupted",
                        e
                );
            }
        }


        // -----------------------------------------------------
        // Scenario #8 - High Value Order
        // -----------------------------------------------------

        if (message.getTotalAmount() != null
                && message.getTotalAmount()
                .compareTo(java.math.BigDecimal.valueOf(10000)) > 0) {

            log.warn(
                    "[CONSUMER] " +
                            "[ERROR_CODE=HIGH_VALUE_ORDER] " +
                            "High value order orderId={} amount={}",
                    orderId,
                    message.getTotalAmount()
            );

            order.setStatus(
                    Order.OrderStatus.FAILED
            );

            order.setFailureReason(
                    "Order amount exceeds allowed threshold"
            );

            order.setProcessedAt(
                    LocalDateTime.now()
            );

            orderRepository.save(order);

            saveAudit(
                    orderId,
                    "PROCESS_ORDER",
                    "ERROR",
                    "High value order detected",
                    "HIGH_VALUE_ORDER"
            );

            return;
        }


        // -----------------------------------------------------
        // Normal successful processing
        // -----------------------------------------------------

        order.setStatus(
                Order.OrderStatus.FULFILLED
        );

        order.setProcessedAt(
                LocalDateTime.now()
        );

        order.setFailureReason(null);

        orderRepository.save(order);

        saveAudit(
                orderId,
                "PROCESS_ORDER",
                "SUCCESS",
                "Order processed successfully",
                null
        );

        log.info(
                "[CONSUMER] Order fulfilled successfully orderId={}",
                orderId
        );
    }


    // =========================================================
    // Audit Helper
    // =========================================================

    private void saveAudit(
            String orderId,
            String action,
            String outcome,
            String details,
            String errorCode) {

        AuditLog auditLog = AuditLog.builder()
                .orderId(orderId)
                .action(action)
                .outcome(outcome)
                .details(details)
                .errorCode(errorCode)
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }
}
