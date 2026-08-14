package com.tieto.poc.ai_servicenow.exception;

public class OptimisticLockSimulationException extends RuntimeException {

    public OptimisticLockSimulationException(String message) {
        super(message);
    }
}
