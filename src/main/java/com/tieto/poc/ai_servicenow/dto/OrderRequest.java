package com.tieto.poc.ai_servicenow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class OrderRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Product code is required")
    private String productCode;

    @Min(
            value = 1,
            message = "Quantity must be greater than zero"
    )
    private Integer quantity;

    @DecimalMin(
            value = "0.01",
            message = "Unit price must be greater than zero"
    )
    private BigDecimal unitPrice;

    private String priority;

    private String shippingAddress;
}
