package com.tieto.poc.ai_servicenow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "order_id",
            nullable = false
    )
    private String orderId;

    @Column(
            name = "action",
            nullable = false
    )
    private String action;

    @Column(
            name = "outcome",
            nullable = false
    )
    private String outcome;

    @Column(
            name = "details",
            length = 2000
    )
    private String details;

    @Column(
            name = "error_code"
    )
    private String errorCode;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}