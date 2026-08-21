package com.tieto.poc.ai_servicenow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tieto.poc.ai_servicenow.dto.DynatraceProblemDetail;
import com.tieto.poc.ai_servicenow.dto.EnrichedIncident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class IncidentEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentEnrichmentService.class);

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final int maxLogLines;
    private final RestTemplate restTemplate;
    private final String azureOpenAiEndpoint;
    private final String azureOpenAiApiKey;
    private final String azureOpenAiModel;
    private final String azureOpenAiApiVersion;

    @Autowired
    public IncidentEnrichmentService(
            ChatModel chatModel,
            RestTemplate restTemplate,
            @Value("${spring.ai.azure.openai.endpoint:}") String azureOpenAiEndpoint,
            @Value("${spring.ai.azure.openai.api-key:}") String azureOpenAiApiKey,
            @Value("${spring.ai.azure.openai.model:gpt-4o-mini}") String azureOpenAiModel,
            @Value("${app.enrichment.azure-api-version:2024-02-15-preview}") String azureOpenAiApiVersion,
            @Value("${app.enrichment.max-log-lines:5}") int maxLogLines
    ) {
        this(
                chatModel,
                new ObjectMapper(),
                maxLogLines,
                restTemplate,
                azureOpenAiEndpoint,
                azureOpenAiApiKey,
                azureOpenAiModel,
                azureOpenAiApiVersion
        );
    }

    public IncidentEnrichmentService(
            ChatModel chatModel,
            ObjectMapper objectMapper,
            int maxLogLines
    ) {
        this(chatModel, objectMapper, maxLogLines, null, "", "", "gpt-4o-mini", "2024-02-15-preview");
    }

    public IncidentEnrichmentService(
            ChatModel chatModel,
            ObjectMapper objectMapper,
            int maxLogLines,
            RestTemplate restTemplate,
            String azureOpenAiEndpoint,
            String azureOpenAiApiKey,
            String azureOpenAiModel,
            String azureOpenAiApiVersion
    ) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.maxLogLines = maxLogLines;
        this.restTemplate = restTemplate;
        this.azureOpenAiEndpoint = azureOpenAiEndpoint;
        this.azureOpenAiApiKey = azureOpenAiApiKey;
        this.azureOpenAiModel = azureOpenAiModel;
        this.azureOpenAiApiVersion = azureOpenAiApiVersion;
    }

    public EnrichedIncident enrich(DynatraceProblemDetail problem, List<String> logs) {
        String prompt = buildPrompt(problem, logs);
        log.info(
                "Starting incident enrichment: title='{}', affectedEntity='{}', inputLogCount={}, sampledLogCount={}, promptLength={}, wiremockConfigured={}, chatModelConfigured={}",
                summarize(problem != null ? problem.getTitle() : null, 120),
                summarize(problem != null ? problem.getAffectedEntityId() : null, 80),
                logs != null ? logs.size() : 0,
                countSampledLogs(logs),
                prompt.length(),
                isLocalHttpWiremockConfigured(),
                chatModel != null
        );

        if (isLocalHttpWiremockConfigured()) {
            log.info(
                    "Using local WireMock enrichment endpoint='{}', model='{}', apiVersion='{}'",
                    sanitizeEndpoint(azureOpenAiEndpoint),
                    azureOpenAiModel,
                    azureOpenAiApiVersion
            );
            try {
                EnrichedIncident wiremockEnriched = enrichViaWiremock(prompt);
                if (wiremockEnriched != null && StringUtils.hasText(wiremockEnriched.getShortDescription())) {
                    log.info(
                            "WireMock enrichment response received successfully: shortDescription='{}', category='{}', urgency={}, impact={}, recommendedActionCount={}",
                            summarize(wiremockEnriched.getShortDescription(), 120),
                            wiremockEnriched.getCategory(),
                            wiremockEnriched.getUrgency(),
                            wiremockEnriched.getImpact(),
                            wiremockEnriched.getRecommendedActions() != null ? wiremockEnriched.getRecommendedActions().size() : 0
                    );
                    return wiremockEnriched;
                }
                log.warn("WireMock enrichment returned no usable shortDescription. Falling back to Spring AI client.");
            } catch (Exception ex) {
                log.warn("WireMock enrichment failed, falling back to Spring AI client", ex);
            }
        }

        if (chatModel != null && StringUtils.hasText(prompt)) {
            log.info(
                    "Using Spring AI chat model for enrichment: endpoint='{}', model='{}'",
                    sanitizeEndpoint(azureOpenAiEndpoint),
                    azureOpenAiModel
            );
            try {
                ChatResponse response = chatModel.call(new Prompt(prompt));
                log.info("Azure/OpenAI enrichment response: {}", response);
                if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
                    String json = response.getResult().getOutput().getText();
                    if (StringUtils.hasText(json)) {
                        EnrichedIncident enriched = objectMapper.readValue(json, EnrichedIncident.class);
                        if (enriched != null && enriched.getShortDescription() != null) {
                            log.info(
                                    "Azure/OpenAI enrichment parsed successfully: shortDescription='{}', category='{}', urgency={}, impact={}, recommendedActionCount={}",
                                    summarize(enriched.getShortDescription(), 120),
                                    enriched.getCategory(),
                                    enriched.getUrgency(),
                                    enriched.getImpact(),
                                    enriched.getRecommendedActions() != null ? enriched.getRecommendedActions().size() : 0
                            );
                            return enriched;
                        }
                        log.warn("Azure/OpenAI enrichment JSON parsed but no usable shortDescription was present.");
                    }
                    log.warn("Azure/OpenAI enrichment returned an empty text payload.");
                }
                log.warn("Azure/OpenAI enrichment response did not contain a result payload.");
            } catch (Exception ex) {
                log.warn("Azure/OpenAI enrichment failed, falling back to heuristic enrichment", ex);
            }
        } else {
            log.warn(
                    "Spring AI chat model enrichment skipped: chatModelConfigured={}, promptPresent={}",
                    chatModel != null,
                    StringUtils.hasText(prompt)
            );
        }

        EnrichedIncident heuristicEnriched = heuristicEnrich(problem, logs);
        log.info(
                "Heuristic enrichment used: shortDescription='{}', category='{}', urgency={}, impact={}, recommendedActionCount={}",
                summarize(heuristicEnriched.getShortDescription(), 120),
                heuristicEnriched.getCategory(),
                heuristicEnriched.getUrgency(),
                heuristicEnriched.getImpact(),
                heuristicEnriched.getRecommendedActions() != null ? heuristicEnriched.getRecommendedActions().size() : 0
        );
        return heuristicEnriched;
    }

    private boolean isLocalHttpWiremockConfigured() {
        return restTemplate != null
                && StringUtils.hasText(azureOpenAiEndpoint)
                && azureOpenAiEndpoint.startsWith("http://")
                && StringUtils.hasText(azureOpenAiModel);
    }

    private EnrichedIncident enrichViaWiremock(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(azureOpenAiApiKey)) {
            headers.set("api-key", azureOpenAiApiKey);
        }

        Map<String, Object> payload = Map.of(
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        String url = String.format(
                "%s/openai/deployments/%s/chat/completions?api-version=%s",
                azureOpenAiEndpoint.replaceAll("/+$", ""),
                azureOpenAiModel,
                azureOpenAiApiVersion
        );
        log.info(
                "Sending WireMock enrichment request: url='{}', hasApiKey={}, promptLength={}",
                url,
                StringUtils.hasText(azureOpenAiApiKey),
                prompt.length()
        );
        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
        log.info(
                "WireMock enrichment HTTP response received: status={}, bodyLength={}",
                response.getStatusCode().value(),
                response.getBody() != null ? response.getBody().length() : 0
        );
        if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
            throw new IllegalStateException("WireMock returned an empty or non-success response");
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (!contentNode.isTextual()) {
            throw new IllegalStateException("WireMock response did not include choices[0].message.content");
        }
        return objectMapper.readValue(contentNode.asText(), EnrichedIncident.class);
    }

    private EnrichedIncident heuristicEnrich(DynatraceProblemDetail problem, List<String> logs) {
        EnrichedIncident e = new EnrichedIncident();
        e.setShortDescription(problem != null && problem.getTitle() != null ? problem.getTitle() : "Application issue detected");
        StringBuilder desc = new StringBuilder();
        if (problem != null && problem.getDescription() != null) {
            desc.append(problem.getDescription());
        } else {
            desc.append("No detail available.");
        }
        if (logs != null && !logs.isEmpty()) {
            desc.append("\n\nRelevant logs:\n");
            for (String line : logs) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                desc.append(line).append("\n");
            }
        }
        e.setDescription(desc.toString());
        e.setUrgency(2);
        e.setImpact(2);
        e.setCategory("application");
        e.setRecommendedActions(List.of("Check application logs","Review recent deployments","Investigate database connectivity"));
        return e;
    }

    private String buildPrompt(DynatraceProblemDetail problem, List<String> logs) {
        if (problem == null) {
            return "Create a JSON incident summary with fields shortDescription, description, urgency, impact, category, recommendedActions.";
        }

        List<String> logSamples = new ArrayList<>();
        if (logs != null) {
            for (String log : logs) {
                if (log != null && !log.isBlank()) {
                    logSamples.add(log);
                    if (logSamples.size() >= maxLogLines) {
                        break;
                    }
                }
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Title: ").append(problem.getTitle() != null ? problem.getTitle() : "Application issue detected").append("\n");
        builder.append("Description: ").append(problem.getDescription() != null ? problem.getDescription() : "No detail available.").append("\n");
        builder.append("Affected entity: ").append(problem.getAffectedEntityId() != null ? problem.getAffectedEntityId() : "unknown").append("\n");
        builder.append("Relevant logs:\n");
        if (logSamples.isEmpty()) {
            builder.append("No logs available.\n");
        } else {
            for (String logLine : logSamples) {
                builder.append("- ").append(logLine).append("\n");
            }
        }
        builder.append("\nReturn valid JSON only with fields: shortDescription, description, urgency, impact, category, recommendedActions.");
        return builder.toString();
    }

    private int countSampledLogs(List<String> logs) {
        if (logs == null) {
            return 0;
        }
        int count = 0;
        for (String logLine : logs) {
            if (StringUtils.hasText(logLine)) {
                count++;
                if (count >= maxLogLines) {
                    break;
                }
            }
        }
        return count;
    }

    private String sanitizeEndpoint(String endpoint) {
        return StringUtils.hasText(endpoint) ? endpoint.replaceAll("/+$", "") : "<not-configured>";
    }

    private String summarize(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "<empty>";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength - 3) + "...";
    }
}
