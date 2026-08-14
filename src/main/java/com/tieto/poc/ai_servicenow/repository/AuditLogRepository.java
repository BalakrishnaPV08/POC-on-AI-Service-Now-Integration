package com.tieto.poc.ai_servicenow.repository;

import com.tieto.poc.ai_servicenow.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByOrderId(String orderId);

    List<AuditLog> findByOrderIdOrderByCreatedAtDesc(
            String orderId
    );
}
