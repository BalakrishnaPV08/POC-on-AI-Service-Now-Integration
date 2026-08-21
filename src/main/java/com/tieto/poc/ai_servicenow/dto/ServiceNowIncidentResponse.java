package com.tieto.poc.ai_servicenow.dto;

import lombok.Data;

@Data
public class ServiceNowIncidentResponse {
    private String sysId;
    private String incidentNumber;
    private String rawJson;
}
