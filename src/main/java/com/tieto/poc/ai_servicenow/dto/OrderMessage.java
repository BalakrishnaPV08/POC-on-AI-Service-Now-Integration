package com.tieto.poc.ai_servicenow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage {

    private String orderId;

    private String customerId;

    private String productCode;

    private Integer quantity;

    private BigDecimal totalAmount;

    private String priority;

    /*
     * Used by the Error Simulator to deliberately
     * trigger consumer-side failures.
     */
    private boolean simulateError;

    /*
     * Examples:
     *
     * PROCESSING_ERROR
     * SLOW_PROCESSING
     */
    private String errorType;
}
