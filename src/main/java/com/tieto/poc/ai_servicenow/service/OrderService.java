package com.tieto.poc.ai_servicenow.service;

import com.tieto.poc.ai_servicenow.dto.OrderMessage;
import com.tieto.poc.ai_servicenow.dto.OrderRequest;
import com.tieto.poc.ai_servicenow.messaging.OrderProducer;
import com.tieto.poc.ai_servicenow.model.AuditLog;
import com.tieto.poc.ai_servicenow.model.Order;
import com.tieto.poc.ai_servicenow.repository.AuditLogRepository;
import com.tieto.poc.ai_servicenow.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final AuditLogRepository auditLogRepository;
    private final OrderProducer orderProducer;

    private static final AtomicInteger ORDER_COUNTER =
            new AtomicInteger(0);

    // =========================================================
    // NORMAL ORDER FLOW
    // =========================================================

    @Transactional
    public Order createOrder(OrderRequest request) {

        log.info(
                "[SERVICE] Creating order for customerId={}",
                request.getCustomerId()
        );

        validateOrderRequest(request);

        String orderId = generateOrderId();

        BigDecimal totalAmount =
                request.getUnitPrice()
                        .multiply(
                                BigDecimal.valueOf(
                                        request.getQuantity()
                                )
                        );

        Order order = Order.builder()
                .orderId(orderId)
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .productCode(request.getProductCode())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .totalAmount(totalAmount)
                .priority(
                        request.getPriority() != null
                                ? request.getPriority()
                                : "NORMAL"
                )
                .shippingAddress(
                        request.getShippingAddress()
                )
                .status(Order.OrderStatus.PENDING)
                .build();

        Order savedOrder =
                orderRepository.save(order);

        saveAudit(
                orderId,
                "ORDER_CREATED",
                "SUCCESS",
                "Order created successfully",
                null
        );

        OrderMessage message = OrderMessage.builder()
                .orderId(orderId)
                .customerId(request.getCustomerId())
                .productCode(request.getProductCode())
                .quantity(request.getQuantity())
                .totalAmount(totalAmount)
                .priority(
                        request.getPriority() != null
                                ? request.getPriority()
                                : "NORMAL"
                )
                .simulateError(false)
                .build();

        orderProducer.sendOrder(message);

        log.info(
                "[SERVICE] Order created and published orderId={}",
                orderId
        );

        return savedOrder;
    }


    // =========================================================
    // GET ORDER
    // =========================================================

    @Transactional(readOnly = true)
    public Order getOrder(String orderId) {

        return orderRepository
                .findByOrderId(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Order not found: " + orderId
                        )
                );
    }


    // =========================================================
    // GET ALL ORDERS
    // =========================================================

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {

        return orderRepository.findAll();
    }


    // =========================================================
    // GET ORDERS BY STATUS
    // =========================================================

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(
            Order.OrderStatus status) {

        return orderRepository.findByStatus(status);
    }


    // =========================================================
    // GET AUDIT LOGS
    // =========================================================

    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogs(
            String orderId) {

        return auditLogRepository
                .findByOrderIdOrderByCreatedAtDesc(orderId);
    }


    // =========================================================
    // SCENARIO #1
    // Duplicate Order / Database Constraint Error
    // =========================================================

    @Transactional
    public void triggerDuplicateOrder() {

        String orderId = "DUPLICATE-ORDER-001";

        log.error(
                "[SCENARIO-1] " +
                        "[ERROR_CODE=DUPLICATE_ORDER] " +
                        "Triggering duplicate order error orderId={}",
                orderId
        );

        Order firstOrder = Order.builder()
                .orderId(orderId)
                .customerId("DUP-CUSTOMER-001")
                .customerName("Duplicate Customer")
                .productCode("DUP-PRODUCT")
                .quantity(1)
                .unitPrice(BigDecimal.TEN)
                .totalAmount(BigDecimal.TEN)
                .priority("NORMAL")
                .status(Order.OrderStatus.PENDING)
                .build();

        orderRepository.saveAndFlush(firstOrder);

        Order duplicateOrder = Order.builder()
                .orderId(orderId)
                .customerId("DUP-CUSTOMER-002")
                .customerName("Duplicate Customer")
                .productCode("DUP-PRODUCT")
                .quantity(1)
                .unitPrice(BigDecimal.TEN)
                .totalAmount(BigDecimal.TEN)
                .priority("NORMAL")
                .status(Order.OrderStatus.PENDING)
                .build();

        try {

            orderRepository.saveAndFlush(
                    duplicateOrder
            );

        } catch (DataIntegrityViolationException ex) {

            log.error(
                    "[SCENARIO-1] " +
                            "[ERROR_CODE=DUPLICATE_ORDER] " +
                            "Duplicate orderId={}",
                    orderId,
                    ex
            );

            saveAudit(
                    orderId,
                    "DUPLICATE_ORDER",
                    "ERROR",
                    "Duplicate order ID detected",
                    "DUPLICATE_ORDER"
            );

            throw ex;
        }
    }


    // =========================================================
    // SCENARIO #3
    // Slow Database Query
    // =========================================================

    @Transactional(readOnly = true)
    public void triggerSlowQuery() {

        log.warn(
                "[SCENARIO-3] " +
                        "[ERROR_CODE=SLOW_DATABASE_QUERY] " +
                        "Starting slow database operation"
        );

        long startTime =
                System.currentTimeMillis();

        orderRepository.findAll();

        try {

            Thread.sleep(6000);

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Slow query simulation interrupted",
                    ex
            );
        }

        long duration =
                System.currentTimeMillis() - startTime;

        log.warn(
                "[SCENARIO-3] " +
                        "[ERROR_CODE=SLOW_DATABASE_QUERY] " +
                        "Database operation completed durationMs={}",
                duration
        );
    }


    // =========================================================
    // SCENARIO #4
    // NullPointerException
    // =========================================================

    public void triggerNullPointerException() {

        log.error(
                "[SCENARIO-4] " +
                        "[ERROR_CODE=NULL_POINTER_EXCEPTION] " +
                        "Triggering NullPointerException"
        );

        String value = null;

        value.length();
    }


    // =========================================================
    // SCENARIO #5
    // Optimistic Lock / Concurrent Update Simulation
    // =========================================================

    @Transactional
    public void triggerOptimisticLock(String orderId) {

        log.error(
                "[SCENARIO-5] " +
                        "[ERROR_CODE=OPTIMISTIC_LOCK] " +
                        "Triggering optimistic lock scenario orderId={}",
                orderId
        );

        Order order = getOrder(orderId);

        Long currentVersion =
                order.getId();

        log.warn(
                "[SCENARIO-5] " +
                        "Simulating concurrent update orderId={} " +
                        "entityId={}",
                orderId,
                currentVersion
        );

        throw new RuntimeException(
                "Simulated optimistic locking conflict " +
                        "for orderId=" + orderId
        );
    }


    // =========================================================
    // SCENARIO #6
    // Poison Message
    // =========================================================

    public void triggerPoisonMessage() {

        log.error(
                "[SCENARIO-6] " +
                        "[ERROR_CODE=POISON_MESSAGE] " +
                        "Triggering malformed RabbitMQ message"
        );

        orderProducer.sendPoisonMessage();
    }


    // =========================================================
    // SCENARIO #7
    // Consumer Processing Failure
    // =========================================================

    @Transactional
    public Order triggerConsumerFailure() {

        Order order =
                createSimulatorOrder(
                        "PROCESSING_ERROR"
                );

        OrderMessage message =
                buildErrorMessage(
                        order,
                        "PROCESSING_ERROR"
                );

        orderProducer.sendErrorMessage(
                message,
                "PROCESSING_ERROR"
        );

        saveAudit(
                order.getOrderId(),
                "CONSUMER_ERROR_SIMULATION",
                "TRIGGERED",
                "Consumer processing failure triggered",
                "CONSUMER_PROCESSING_FAILURE"
        );

        return order;
    }


    // =========================================================
    // SCENARIO #8
    // High Value Order
    // =========================================================

    @Transactional
    public Order triggerHighValueOrder() {

        OrderRequest request =
                OrderRequest.builder()
                        .customerId("HIGH-VALUE-CUSTOMER")
                        .customerName("High Value Customer")
                        .productCode("PREMIUM-PRODUCT")
                        .quantity(1)
                        .unitPrice(
                                new BigDecimal("15000.00")
                        )
                        .priority("HIGH")
                        .shippingAddress(
                                "High Value Customer Address"
                        )
                        .build();

        return createOrder(request);
    }


    // =========================================================
    // SCENARIO #9
    // Slow Consumer Processing
    // =========================================================

    @Transactional
    public Order triggerSlowConsumer() {

        Order order =
                createSimulatorOrder(
                        "SLOW_PROCESSING"
                );

        OrderMessage message =
                buildErrorMessage(
                        order,
                        "SLOW_PROCESSING"
                );

        orderProducer.sendErrorMessage(
                message,
                "SLOW_PROCESSING"
        );

        saveAudit(
                order.getOrderId(),
                "SLOW_CONSUMER_SIMULATION",
                "TRIGGERED",
                "Slow consumer processing triggered",
                "SLOW_CONSUMER_PROCESSING"
        );

        return order;
    }


    // =========================================================
    // SCENARIO #10
    // Database Connection Failure
    // =========================================================

    public void triggerDatabaseConnectionFailure() {

        log.error(
                "[SCENARIO-10] " +
                        "[ERROR_CODE=DATABASE_CONNECTION_FAILURE] " +
                        "Simulating database connection failure"
        );

        throw new DataAccessResourceFailureException(
                "Simulated database connection failure"
        );
    }


    // =========================================================
    // Helper - Create Simulator Order
    // =========================================================

    private Order createSimulatorOrder(
            String errorType) {

        String orderId =
                generateOrderId();

        Order order = Order.builder()
                .orderId(orderId)
                .customerId("SIMULATOR-CUSTOMER")
                .customerName("Error Simulator")
                .productCode("SIMULATOR-" + errorType)
                .quantity(1)
                .unitPrice(
                        new BigDecimal("100.00")
                )
                .totalAmount(
                        new BigDecimal("100.00")
                )
                .priority("NORMAL")
                .status(Order.OrderStatus.PENDING)
                .shippingAddress("Simulator Address")
                .build();

        Order savedOrder =
                orderRepository.save(order);

        log.info(
                "[SIMULATOR] Created orderId={} " +
                        "for errorType={}",
                orderId,
                errorType
        );

        return savedOrder;
    }


    // =========================================================
    // Helper - Build Error Message
    // =========================================================

    private OrderMessage buildErrorMessage(
            Order order,
            String errorType) {

        return OrderMessage.builder()
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .productCode(order.getProductCode())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .priority(order.getPriority())
                .simulateError(true)
                .errorType(errorType)
                .build();
    }


    // =========================================================
    // Helper - Validation
    // =========================================================

    private void validateOrderRequest(
            OrderRequest request) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Order request cannot be null"
            );
        }

        if (request.getQuantity() == null
                || request.getQuantity() <= 0) {

            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }

        if (request.getUnitPrice() == null
                || request.getUnitPrice()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Unit price must be greater than zero"
            );
        }
    }


    // =========================================================
    // Helper - Generate Order ID
    // =========================================================

    private String generateOrderId() {

        String date =
                LocalDateTime.now()
                        .format(
                                DateTimeFormatter
                                        .ofPattern("yyyyMMdd")
                        );

        int sequence =
                ORDER_COUNTER.incrementAndGet();

        return String.format(
                "ORD-%s-%04d",
                date,
                sequence
        );
    }


    // =========================================================
    // Helper - Audit
    // =========================================================

    private void saveAudit(
            String orderId,
            String action,
            String outcome,
            String details,
            String errorCode) {

        AuditLog auditLog =
                AuditLog.builder()
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
