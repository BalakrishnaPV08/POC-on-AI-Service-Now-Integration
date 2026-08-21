package com.tieto.poc.ai_servicenow.controller;

import com.tieto.poc.ai_servicenow.model.Order;
import com.tieto.poc.ai_servicenow.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/errors")
@RequiredArgsConstructor
public class ErrorSimulatorController {

    private final OrderService orderService;


    // =========================================================
    // Scenario #1 - Duplicate Order
    // =========================================================
    @PostMapping("/duplicate-order/{orderId}")
    public ResponseEntity<Map<String, String>> duplicateOrder(
            @PathVariable String orderId) {

        log.warn(
                "[SIMULATOR] Triggering Scenario #1 - Duplicate Order orderId={}",
                orderId
        );

        orderService.triggerDuplicateOrder(orderId);

        return ResponseEntity.ok(
                Map.of(
                        "scenario", "1",
                        "message", "Duplicate order scenario triggered",
                        "orderId", orderId
                )
        );
    }


    // =========================================================
    // Scenario #3 - Slow Database Query
    // =========================================================

    @PostMapping("/slow-query")
    public ResponseEntity<Map<String, String>> slowQuery() {

        log.warn(
                "[SIMULATOR] Triggering Scenario #3 - Slow Query"
        );

        orderService.triggerSlowQuery();

        return ResponseEntity.ok(
                Map.of(
                        "scenario",
                        "3",
                        "message",
                        "Slow database query scenario triggered"
                )
        );
    }


    // =========================================================
    // Scenario #4 - NullPointerException
    // =========================================================

    @PostMapping("/null-pointer")
    public ResponseEntity<Map<String, String>> nullPointer() {

        log.warn(
                "[SIMULATOR] Triggering Scenario #4 - NullPointerException"
        );

        orderService.triggerNullPointerException();

        return ResponseEntity.ok(
                Map.of(
                        "scenario",
                        "4",
                        "message",
                        "NullPointerException scenario triggered"
                )
        );
    }


    // =========================================================
    // Scenario #5 - Optimistic Lock
    // =========================================================
    @PostMapping("/optimistic-lock/{orderId}")
    public ResponseEntity<Map<String, String>> optimisticLock(
            @PathVariable String orderId) {

        log.warn(
                "[SIMULATOR] Triggering Scenario #5 - Optimistic Lock orderId={}",
                orderId
        );

        orderService.triggerOptimisticLock(orderId);

        return ResponseEntity.ok(
                Map.of(
                        "scenario", "5",
                        "message", "Optimistic lock scenario triggered",
                        "orderId", orderId
                )
        );
    }


    // =========================================================
    // Scenario #6 - Poison RabbitMQ Message
    // =========================================================

    @PostMapping("/poison-message")
    public ResponseEntity<Map<String, String>> poisonMessage() {

        log.warn(
                "[SIMULATOR] Triggering Scenario #6 - Poison Message"
        );

        orderService.triggerPoisonMessage();

        return ResponseEntity.ok(
                Map.of(
                        "scenario",
                        "6",
                        "message",
                        "Poison RabbitMQ message sent"
                )
        );
    }


    // =========================================================
    // Scenario #7 - Consumer Processing Failure
    // =========================================================

    @PostMapping("/consumer-failure")
    public ResponseEntity<Map<String, String>> consumerFailure() {

        log.warn(
                "[SIMULATOR] Triggering Scenario #7 - " +
                        "Consumer Processing Failure"
        );

        var order =
                orderService.triggerConsumerFailure();

        return ResponseEntity.ok(
                Map.of(
                        "scenario",
                        "7",
                        "message",
                        "Consumer processing failure triggered",
                        "orderId",
                        order.getOrderId()
                )
        );
    }


    // =========================================================
    // Scenario #8 - High Value Order
    // =========================================================

    @PostMapping("/high-value-order")
    public ResponseEntity<Map<String, String>> highValueOrder() {

        log.warn(
                "[SIMULATOR] Triggering Scenario #8 - " +
                        "High Value Order"
        );

        var order =
                orderService.triggerHighValueOrder();

        return ResponseEntity.ok(
                Map.of(
                        "scenario",
                        "8",
                        "message",
                        "High value order created",
                        "orderId",
                        order.getOrderId()
                )
        );
    }


    // =========================================================
    // Scenario #9 - Slow Consumer Processing
    // =========================================================

    @PostMapping("/slow-processing")
    public ResponseEntity<Map<String, String>> slowProcessing() {

        log.warn(
                "[SIMULATOR] Triggering Scenario #9 - " +
                        "Slow Consumer Processing"
        );

        var order =
                orderService.triggerSlowConsumer();

        return ResponseEntity.ok(
                Map.of(
                        "scenario",
                        "9",
                        "message",
                        "Slow consumer processing triggered",
                        "orderId",
                        order.getOrderId()
                )
        );
    }


    // =========================================================
    // Scenario #10 - Database Connection Failure
    // =========================================================

    @PostMapping("/database-connection")
    public ResponseEntity<Map<String, String>> databaseConnection() {

        log.warn(
                "[SIMULATOR] Triggering Scenario #10 - " +
                        "Database Connection Failure"
        );

        orderService.triggerDatabaseConnectionFailure();

        return ResponseEntity.ok(
                Map.of(
                        "scenario",
                        "10",
                        "message",
                        "Database connection failure triggered"
                )
        );
    }
}
