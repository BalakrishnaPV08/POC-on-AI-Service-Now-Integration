package com.tieto.poc.ai_servicenow.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final com.tieto.poc.ai_servicenow.service.DynatraceEventService dynatraceEventService;
    private final com.tieto.poc.ai_servicenow.service.ApplicationOperationsAgent applicationOperationsAgent;

    // =========================================================
    // Order Not Found
    // =========================================================

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotFound(
            OrderNotFoundException ex) {

        log.error(
                "[ERROR_CODE=ORDER_NOT_FOUND] {}",
                ex.getMessage()
        );

        sendExceptionToDynatrace(ex, "ORDER_NOT_FOUND");

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "ORDER_NOT_FOUND",
                ex.getMessage()
        );
    }


    // =========================================================
    // Duplicate Order
    // =========================================================

    @ExceptionHandler(DuplicateOrderException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateOrder(
            DuplicateOrderException ex) {

        log.error(
                "[ERROR_CODE=DUPLICATE_ORDER] {}",
                ex.getMessage()
        );

        sendExceptionToDynatrace(ex, "DUPLICATE_ORDER");

        return buildResponse(
                HttpStatus.CONFLICT,
                "DUPLICATE_ORDER",
                ex.getMessage()
        );
    }

    // =========================================================
// Optimistic Lock Error
// =========================================================

    @ExceptionHandler(OptimisticLockSimulationException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(
            OptimisticLockSimulationException ex) {

        log.error(
                "[ERROR_CODE=OPTIMISTIC_LOCK] {}",
                ex.getMessage()
        );

        sendExceptionToDynatrace(ex, "OPTIMISTIC_LOCK");

        return buildResponse(
                HttpStatus.CONFLICT,
                "OPTIMISTIC_LOCK",
                ex.getMessage()
        );
    }


    // =========================================================
    // Validation Error
    // =========================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(
            MethodArgumentNotValidException ex) {

        log.error(
                "[ERROR_CODE=VALIDATION_ERROR] " +
                        "Request validation failed"
        );

        sendExceptionToDynatrace(ex, "VALIDATION_ERROR");

        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        validationErrors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "timestamp",
                LocalDateTime.now()
        );

        response.put(
                "status",
                HttpStatus.BAD_REQUEST.value()
        );

        response.put(
                "errorCode",
                "VALIDATION_ERROR"
        );

        response.put(
                "message",
                "Request validation failed"
        );

        response.put(
                "errors",
                validationErrors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }


    // =========================================================
    // Database Integrity Error
    // =========================================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityError(
            DataIntegrityViolationException ex) {

        log.error(
                "[ERROR_CODE=DATABASE_INTEGRITY_ERROR] " +
                        "Database constraint violation",
                ex
        );

        sendExceptionToDynatrace(ex, "DATABASE_INTEGRITY_ERROR");

        return buildResponse(
                HttpStatus.CONFLICT,
                "DATABASE_INTEGRITY_ERROR",
                "Database constraint violation"
        );
    }


    // =========================================================
    // Database Connection Error
    // =========================================================

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseConnectionError(
            DataAccessResourceFailureException ex) {

        log.error(
                "[ERROR_CODE=DATABASE_CONNECTION_FAILURE] " +
                        "Database connection failure",
                ex
        );

        sendExceptionToDynatrace(ex, "DATABASE_CONNECTION_FAILURE");

        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "DATABASE_CONNECTION_FAILURE",
                "Database connection is currently unavailable"
        );
    }


    // =========================================================
    // Illegal Argument
    // =========================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {

        log.error(
                "[ERROR_CODE=INVALID_REQUEST] {}",
                ex.getMessage()
        );

        sendExceptionToDynatrace(ex, "INVALID_REQUEST");

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                ex.getMessage()
        );
    }


    // =========================================================
    // Generic Application Error
    // =========================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(
            Exception ex) {

        log.error(
                "[ERROR_CODE=APPLICATION_ERROR] " +
                        "Unhandled application exception",
                ex
        );

        sendExceptionToDynatrace(ex, "APPLICATION_ERROR");

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "APPLICATION_ERROR",
                "An unexpected error occurred"
        );
    }

    private void sendExceptionToDynatrace(Exception ex, String errorCode) {
        try {
            java.util.Map<String, Object> props = new java.util.LinkedHashMap<>();
            props.put("message", ex.getMessage());
            props.put("exception", ex.getClass().getName());
            props.put("errorCode", errorCode);
            java.io.StringWriter sw = new java.io.StringWriter();
            ex.printStackTrace(new java.io.PrintWriter(sw));
            String stack = sw.toString();
            props.put("stacktrace", stack.length() > 4000 ? stack.substring(0, 4000) + "..." : stack);
            props.put("timestamp", java.time.Instant.now().toString());

            dynatraceEventService.sendEvent(props, "Application Exception: " + ex.getClass().getSimpleName());

            applicationOperationsAgent.processExceptionIncident(
                    errorCode,
                    ex.getClass().getName(),
                    ex.getMessage(),
                    stack
            );
        } catch (Exception e) {
            log.warn("Failed to forward exception to Dynatrace or create incident", e);
        }
    }


    // =========================================================
    // Common Response Builder
    // =========================================================

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String errorCode,
            String message) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "timestamp",
                LocalDateTime.now()
        );

        response.put(
                "status",
                status.value()
        );

        response.put(
                "errorCode",
                errorCode
        );

        response.put(
                "message",
                message
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}