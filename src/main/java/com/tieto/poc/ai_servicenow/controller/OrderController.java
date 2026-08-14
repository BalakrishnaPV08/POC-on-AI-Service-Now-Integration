package com.tieto.poc.ai_servicenow.controller;

import com.tieto.poc.ai_servicenow.dto.OrderRequest;
import com.tieto.poc.ai_servicenow.model.AuditLog;
import com.tieto.poc.ai_servicenow.model.Order;
import com.tieto.poc.ai_servicenow.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // =========================================================
    // Create Order
    // =========================================================

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @Valid @RequestBody OrderRequest request) {

        log.info(
                "[CONTROLLER] Creating order customerId={} productCode={}",
                request.getCustomerId(),
                request.getProductCode()
        );

        Order order =
                orderService.createOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(order);
    }


    // =========================================================
    // Get All Orders
    // =========================================================

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {

        log.info(
                "[CONTROLLER] Fetching all orders"
        );

        return ResponseEntity.ok(
                orderService.getAllOrders()
        );
    }


    // =========================================================
    // Get Order By Order ID
    // =========================================================

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(
            @PathVariable String orderId) {

        log.info(
                "[CONTROLLER] Fetching orderId={}",
                orderId
        );

        return ResponseEntity.ok(
                orderService.getOrder(orderId)
        );
    }


    // =========================================================
    // Get Orders By Status
    // =========================================================

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(
            @PathVariable Order.OrderStatus status) {

        log.info(
                "[CONTROLLER] Fetching orders status={}",
                status
        );

        return ResponseEntity.ok(
                orderService.getOrdersByStatus(status)
        );
    }


    // =========================================================
    // Get Audit Logs
    // =========================================================

    @GetMapping("/{orderId}/audit")
    public ResponseEntity<List<AuditLog>> getAuditLogs(
            @PathVariable String orderId) {

        log.info(
                "[CONTROLLER] Fetching audit logs orderId={}",
                orderId
        );

        return ResponseEntity.ok(
                orderService.getAuditLogs(orderId)
        );
    }
}
