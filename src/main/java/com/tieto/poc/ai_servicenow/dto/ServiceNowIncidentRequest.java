package com.tieto.poc.ai_servicenow.dto;

import lombok.Data;

@Data
public class ServiceNowIncidentRequest {
    private String short_description;
    private String description;
    private Integer urgency;
    private Integer impact;
    private String category;
    private String correlation_id;
}
