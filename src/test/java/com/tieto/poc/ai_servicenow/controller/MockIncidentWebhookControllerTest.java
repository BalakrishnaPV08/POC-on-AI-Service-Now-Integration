package com.tieto.poc.ai_servicenow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tieto.poc.ai_servicenow.dto.DynatraceCloudEvent;
import com.tieto.poc.ai_servicenow.dto.DynatraceProblemEvent;
import com.tieto.poc.ai_servicenow.service.ApplicationOperationsAgent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MockIncidentWebhookControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsValidWebhookAndDelegates() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/dynatrace-sample-problem.json"), StandardCharsets.UTF_8);
        DynatraceCloudEvent cloudEvent = objectMapper.readValue(json, DynatraceCloudEvent.class);

        ApplicationOperationsAgent agent = mock(ApplicationOperationsAgent.class);
        IncidentWebhookController controller = new IncidentWebhookController(agent);

        // set private webhookAuthToken via reflection to simulate configured token
        setPrivateField(controller, "webhookAuthToken", "test-token");

        var resp = controller.processIncident("test-token", cloudEvent);
        assertEquals(200, resp.getStatusCode().value());

        verify(agent, times(1)).processIncident(any(DynatraceProblemEvent.class));
    }

    @Test
    void rejectsWhenAuthInvalid() throws Exception {
        String json = Files.readString(Paths.get("src/test/resources/dynatrace-sample-problem.json"), StandardCharsets.UTF_8);
        DynatraceCloudEvent cloudEvent = objectMapper.readValue(json, DynatraceCloudEvent.class);

        ApplicationOperationsAgent agent = mock(ApplicationOperationsAgent.class);
        IncidentWebhookController controller = new IncidentWebhookController(agent);

        setPrivateField(controller, "webhookAuthToken", "test-token");

        var resp = controller.processIncident("wrong-token", cloudEvent);
        assertEquals(401, resp.getStatusCode().value());

        verify(agent, never()).processIncident(any());
    }

    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }
}
