package com.tieto.poc.ai_servicenow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynatraceCloudEvent {
    private String specversion;
    private String id;
    private String source;
    private String type;
    private String datacontenttype;
    private DynatraceCloudEventData data;

    public DynatraceProblemEvent toProblemEvent() {
        DynatraceProblemEvent event = new DynatraceProblemEvent();

        // Determine problemId: prefer CloudEvent id; otherwise synthesize a stable id based on payload
        String pid;
        if (id != null && !id.isEmpty()) {
            pid = id;
        } else {
            String base = (data != null ? data.getMessage() : "") + "|" + (data != null ? data.getHost_name() : "") + "|" + (data != null && data.getLevel_value() != null ? data.getLevel_value() : "");
            pid = "log-" + UUID.nameUUIDFromBytes(base.getBytes()).toString();
        }
        event.setProblemId(pid);

        // Map severity -> status
        String level = data != null && data.getLevel() != null ? data.getLevel() : data != null ? data.getLoglevel() : null;
        if (level != null && level.equalsIgnoreCase("ERROR")) {
            event.setStatus("OPEN");
        } else if (level != null && level.equalsIgnoreCase("WARN")) {
            event.setStatus("WARN");
        } else {
            event.setStatus("INFO");
        }

        // Prefer log_source, fallback to host_name
        String affected = null;
        if (data != null) {
            affected = data.getLog_source() != null && !data.getLog_source().isEmpty() ? data.getLog_source() : data.getHost_name();
        }
        event.setAffectedEntity(affected);

        event.setEventType(type != null ? type : "error_event");

        // Compose a compact rawPayload for downstream enrichment
        String raw = "";
        if (data != null) {
            raw = String.format("message=%s;logger=%s;thread=%s;host=%s", data.getMessage(), data.getLogger_name(), data.getThread_name(), data.getLog_source());
        }
        event.setRawPayload(raw);

        return event;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DynatraceCloudEventData {
        private String message;
        @JsonProperty("logger_name")
        private String logger_name;
        @JsonProperty("thread_name")
        private String thread_name;
        private String level;
        @JsonProperty("level_value")
        private Integer level_value;
        private String service;
        private String environment;
        @JsonProperty("host_name")
        private String host_name;
        @JsonProperty("log_source")
        private String log_source;
        private String loglevel;
    }
}
