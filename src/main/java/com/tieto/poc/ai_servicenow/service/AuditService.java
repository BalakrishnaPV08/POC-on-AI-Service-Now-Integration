package com.tieto.poc.ai_servicenow.service;

import com.tieto.poc.ai_servicenow.model.AuditLog;
import com.tieto.poc.ai_servicenow.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAudit(
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

        log.info(
                "[AUDIT] orderId={} action={} outcome={} errorCode={}",
                orderId,
                action,
                outcome,
                errorCode
        );
    }
}