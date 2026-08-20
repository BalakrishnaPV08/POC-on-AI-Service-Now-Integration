package com.tieto.poc.ai_servicenow.service;

import com.tieto.poc.ai_servicenow.dto.DynatraceProblemEvent;
import com.tieto.poc.ai_servicenow.dto.DynatraceProblemDetail;
import com.tieto.poc.ai_servicenow.dto.EnrichedIncident;
import com.tieto.poc.ai_servicenow.dto.ServiceNowIncidentRequest;
import com.tieto.poc.ai_servicenow.dto.ServiceNowIncidentResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationOperationsAgent {

    private static final Logger log = LoggerFactory.getLogger(ApplicationOperationsAgent.class);

    private final DynatraceService dynatraceService;
    private final IncidentEnrichmentService enrichmentService;
    private final ServiceNowService serviceNowService;

    public void processIncident(DynatraceProblemEvent event) {
        if (event == null) return;
        String problemId = event.getProblemId();
        String status = event.getStatus();
        if (status != null && status.equalsIgnoreCase("RESOLVED")) {
            log.info("Skipping resolved event {}", problemId);
            return;
        }

        DynatraceProblemDetail problem = dynatraceService.getProblemById(problemId);
        List<String> logs = dynatraceService.getRecentErrorLogs(problem.getAffectedEntityId());

        EnrichedIncident enriched = enrichmentService.enrich(problem, logs);

        ServiceNowIncidentResponse existing = serviceNowService.findExistingIncident(problemId);
        if (existing != null) {
            log.info("Duplicate incident {} found for problem {}", existing.getIncidentNumber(), problemId);
            return;
        }

        ServiceNowIncidentRequest req = new ServiceNowIncidentRequest();
        req.setShort_description(enriched.getShortDescription());
        req.setDescription(enriched.getDescription());
        req.setUrgency(enriched.getUrgency());
        req.setImpact(enriched.getImpact());
        req.setCategory(enriched.getCategory());
        req.setCorrelation_id(problemId);

        ServiceNowIncidentResponse created = serviceNowService.createIncident(req);
        log.info("[AOA] Incident {} (sys_id={}) created for DT problem {}", created.getIncidentNumber(), created.getSysId(), problemId);
        log.debug("[AOA] Created incident raw response: {}", created.getRawJson());
    }
}
