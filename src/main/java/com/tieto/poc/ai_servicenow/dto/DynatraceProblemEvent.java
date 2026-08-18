package com.tieto.poc.ai_servicenow.dto;

import lombok.Data;

@Data
public class DynatraceProblemEvent {
    private String problemId;
    private String status;
    private String affectedEntity;
    private String eventType;
    private String rawPayload;
}
