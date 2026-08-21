package com.tieto.poc.ai_servicenow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class DynatraceEventService {

    private static final Logger log = LoggerFactory.getLogger(DynatraceEventService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${dynatrace.api-url:}")
    private String dtApiUrl;

    @Value("${dynatrace.api-token:}")
    private String dtApiToken;

    public boolean isConfigured() {
        return dtApiUrl != null && !dtApiUrl.isEmpty() && dtApiToken != null && !dtApiToken.isEmpty();
    }

    @Async
    public CompletableFuture<Boolean> sendEvent(Map<String, Object> properties, String title) {
        try {
            if (!isConfigured()) {
                log.debug("Dynatrace not configured, skipping event send");
                return CompletableFuture.completedFuture(false);
            }

            String endpoint = dtApiUrl;
            if (!endpoint.endsWith("/")) endpoint += "/";
            endpoint += "api/v2/events/ingest";

            // Use the single-object event payload format expected by Dynatrace Events API.
            // This matches the working Postman example and avoids the 400 mapping error.
            Map<String, Object> event = new java.util.LinkedHashMap<>();
            event.put("eventType", "ERROR_EVENT");
            event.put("title", title != null ? title : "application-error");
            event.put("timeout", 30);
            event.put("entitySelector", "type(SERVICE),entityName.equals(ai-servicenow)");
            event.put("properties", properties);

            String payload = objectMapper.writeValueAsString(event);
            log.info("Payload being sent to Dynatrace Events API: {}", payload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Api-Token " + dtApiToken);

            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(endpoint, entity, String.class);
            log.info("Dynatrace event send status={} bodyLen={}", resp.getStatusCode().value(), resp.getBody() != null ? resp.getBody().length() : 0);
            return CompletableFuture.completedFuture(true);
        } catch (Exception ex) {
            log.warn("Failed to send Dynatrace event", ex);
            return CompletableFuture.completedFuture(false);
        }
    }
}
