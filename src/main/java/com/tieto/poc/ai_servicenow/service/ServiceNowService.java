package com.tieto.poc.ai_servicenow.service;

import com.tieto.poc.ai_servicenow.dto.ServiceNowIncidentRequest;
import com.tieto.poc.ai_servicenow.dto.ServiceNowIncidentResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class ServiceNowService {

    private static final Logger log = LoggerFactory.getLogger(ServiceNowService.class);

    private final RestTemplate restTemplate;

    @Value("${servicenow.instance-url:}")
    private String snInstanceUrl;

    @Value("${servicenow.username:}")
    private String snUsername;

    @Value("${servicenow.password:}")
    private String snPassword;

    public ServiceNowIncidentResponse findExistingIncident(String correlationId) {
        // For POC scaffold: always return null (no duplicate found)
        return null;
    }

    public ServiceNowIncidentResponse createIncident(ServiceNowIncidentRequest request) {
        // If ServiceNow is not configured return a stub response
        if (snInstanceUrl == null || snInstanceUrl.isEmpty()) {
            ServiceNowIncidentResponse r = new ServiceNowIncidentResponse();
            r.setSysId("stub-sysid-" + System.currentTimeMillis());
            r.setIncidentNumber("INC" + (100000 + (int)(Math.random()*899999)));
            log.info("[AOA] Stub ServiceNow incident {} created", r.getIncidentNumber());
            return r;
        }

        try {
            String url = snInstanceUrl;
            if (!url.endsWith("/")) url += "/";
            url += "api/now/table/incident";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (snUsername != null && !snUsername.isEmpty() && snPassword != null) {
                String auth = snUsername + ":" + snPassword;
                String b64 = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
                headers.set(HttpHeaders.AUTHORIZATION, "Basic " + b64);
            }
            HttpEntity<ServiceNowIncidentRequest> entity = new HttpEntity<>(request, headers);
            // Call ServiceNow and capture full response
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            String resp = response.getBody();

            ServiceNowIncidentResponse r = new ServiceNowIncidentResponse();
            if (resp != null && !resp.isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode root = om.readTree(resp);
                    com.fasterxml.jackson.databind.JsonNode result = root.path("result");
                    if (!result.isMissingNode()) {
                        String number = result.path("number").asText(null);
                        String sysId = result.path("sys_id").asText(null);
                        if (number != null) r.setIncidentNumber(number);
                        if (sysId != null) r.setSysId(sysId);
                    }
                    r.setRawJson(resp);
                } catch (Exception parseEx) {
                    log.warn("Failed to parse ServiceNow response JSON", parseEx);
                }
            }

            // Fallback defaults if parsing didn't set values
            if (r.getIncidentNumber() == null) r.setIncidentNumber("INC-created");
            if (r.getSysId() == null) r.setSysId("created-sysid");

            log.info("[AOA] ServiceNow incident created (number={}, sys_id={})", r.getIncidentNumber(), r.getSysId());
            log.debug("[AOA] ServiceNow create response: {}", r.getRawJson());
            return r;
        } catch (Exception ex) {
            log.error("Failed to create ServiceNow incident", ex);
            ServiceNowIncidentResponse r = new ServiceNowIncidentResponse();
            r.setSysId("error");
            r.setIncidentNumber("ERROR");
            return r;
        }
    }
}
