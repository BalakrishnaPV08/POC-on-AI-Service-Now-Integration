package com.tieto.poc.ai_servicenow.service;

import com.tieto.poc.ai_servicenow.dto.DynatraceProblemDetail;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DynatraceService {

    private static final Logger log = LoggerFactory.getLogger(DynatraceService.class);

    private final RestTemplate restTemplate;

    @Value("${dynatrace.api-url:}")
    private String dtApiUrl;

    @Value("${dynatrace.api-token:}")
    private String dtApiToken;

    public DynatraceProblemDetail getProblemById(String problemId) {
            // Require both URL and token for live calls; otherwise return a stub
            if (dtApiUrl == null || dtApiUrl.isEmpty() || dtApiToken == null || dtApiToken.isEmpty()) {
            DynatraceProblemDetail d = new DynatraceProblemDetail();
            d.setId(problemId);
            d.setTitle("Stub problem title");
                d.setDescription("Dynatrace not configured or missing token; returning stub.");
            d.setAffectedEntityId("unknown");
            d.setRawJson("{}");
            return d;
        }

        try {
            String url = dtApiUrl;
            if (!url.endsWith("/")) url += "/";
            url += "api/v2/problems/" + problemId;

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                headers.set("Authorization", "Api-Token " + dtApiToken);

                HttpEntity<Void> entity = new HttpEntity<>(headers);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                String resp = response.getBody();

            DynatraceProblemDetail d = new DynatraceProblemDetail();
            d.setId(problemId);
            d.setRawJson(resp != null ? resp : "{}");
            d.setTitle("Dynatrace Problem " + problemId);
            d.setDescription("Retrieved problem from Dynatrace");
            d.setAffectedEntityId("unknown");
            return d;
        } catch (Exception ex) {
            log.error("Failed to retrieve Dynatrace problem", ex);
            DynatraceProblemDetail d = new DynatraceProblemDetail();
            d.setId(problemId);
            d.setTitle("Error retrieving problem");
            d.setDescription("Error: " + ex.getMessage());
            d.setAffectedEntityId("unknown");
            d.setRawJson("{}");
            return d;
        }
    }

    public List<String> getRecentErrorLogs(String affectedEntityId) {
        // Return a small stub list if DT not configured
        if (dtApiUrl == null || dtApiUrl.isEmpty()) {
            return List.of("[stub] NullPointerException at com.tieto.OrderService:42",
                    "[stub] Caused by: simulated database timeout");
        }

        // For simplicity don't call logs API in this scaffold
        return List.of();
    }

    public String getProblems(String from, int pageSize) {
        if (dtApiUrl == null || dtApiUrl.isEmpty() || dtApiToken == null || dtApiToken.isEmpty()) {
            return "";
        }
        try {
            String url = dtApiUrl;
            if (!url.endsWith("/")) url += "/";
            url += "api/v2/problems?from=" + from + "&pageSize=" + pageSize;

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("Authorization", "Api-Token " + dtApiToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody() != null ? response.getBody() : "";
        } catch (Exception ex) {
            log.warn("Failed to query Dynatrace problems", ex);
            return "";
        }
    }
}
