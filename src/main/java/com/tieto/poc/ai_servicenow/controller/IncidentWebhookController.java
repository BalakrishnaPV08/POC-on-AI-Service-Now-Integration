package com.tieto.poc.ai_servicenow.controller;

import com.tieto.poc.ai_servicenow.dto.DynatraceProblemEvent;
import com.tieto.poc.ai_servicenow.service.ApplicationOperationsAgent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class IncidentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(IncidentWebhookController.class);

    private final ApplicationOperationsAgent agent;

    @Value("${dynatrace.webhook-auth-token:}")
    private String webhookAuthToken;

    @PostMapping("/process-incident")
    public ResponseEntity<String> processIncident(@RequestHeader(value = "X-Dynatrace-Problem-Authentication", required = false) String authHeader,
                                                  @RequestBody DynatraceProblemEvent event) {
        if (webhookAuthToken != null && !webhookAuthToken.isEmpty()) {
            if (authHeader == null || !authHeader.equals(webhookAuthToken)) {
                log.warn("Invalid Dynatrace webhook auth header");
                return ResponseEntity.status(401).body("Invalid authentication");
            }
        }

        try {
            agent.processIncident(event);
            return ResponseEntity.ok("Accepted");
        } catch (Exception ex) {
            log.error("Failed to process incident", ex);
            // Allow Dynatrace to retry
            return ResponseEntity.status(500).body("Processing failed");
        }
    }
}
