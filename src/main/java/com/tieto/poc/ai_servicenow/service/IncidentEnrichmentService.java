package com.tieto.poc.ai_servicenow.service;

import com.tieto.poc.ai_servicenow.dto.DynatraceProblemDetail;
import com.tieto.poc.ai_servicenow.dto.EnrichedIncident;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentEnrichmentService.class);

    public EnrichedIncident enrich(DynatraceProblemDetail problem, List<String> logs) {
        // Minimal heuristic enrichment for POC: build a short description and recommended actions
        EnrichedIncident e = new EnrichedIncident();
        e.setShortDescription(problem.getTitle() != null ? problem.getTitle() : "Application issue detected");
        StringBuilder desc = new StringBuilder();
        desc.append(problem.getDescription() != null ? problem.getDescription() : "No detail available.");
        if (logs != null && !logs.isEmpty()) {
            desc.append("\n\nRelevant logs:\n");
            for (String l : logs) {
                desc.append(l).append("\n");
            }
        }
        e.setDescription(desc.toString());
        e.setUrgency(2);
        e.setImpact(2);
        e.setCategory("application");
        e.setRecommendedActions(List.of("Check application logs","Review recent deployments","Investigate database connectivity"));
        return e;
    }
}
