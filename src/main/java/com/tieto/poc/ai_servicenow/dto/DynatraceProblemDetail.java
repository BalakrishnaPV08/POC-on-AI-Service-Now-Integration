package com.tieto.poc.ai_servicenow.dto;

import lombok.Data;

@Data
public class DynatraceProblemDetail {
    private String id;
    private String title;
    private String description;
    private String affectedEntityId;
    private String rawJson;
}
