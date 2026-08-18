package com.tieto.poc.ai_servicenow.dto;

import lombok.Data;
import java.util.List;

@Data
public class EnrichedIncident {
    private String shortDescription;
    private String description;
    private Integer urgency;
    private Integer impact;
    private String category;
    private List<String> recommendedActions;
}
