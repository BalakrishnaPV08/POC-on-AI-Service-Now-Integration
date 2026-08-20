package com.tieto.poc.ai_servicenow.controller;

import com.tieto.poc.ai_servicenow.service.DynatraceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dynatrace")
@RequiredArgsConstructor
public class DynatraceController {

    private static final Logger log = LoggerFactory.getLogger(DynatraceController.class);

    private final DynatraceService dynatraceService;

    @GetMapping(value = "/problems", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getProblems(@RequestParam(value = "from", defaultValue = "now-1h") String from,
                                              @RequestParam(value = "pageSize", defaultValue = "50") int pageSize) {
        String body = dynatraceService.getProblems(from, pageSize);
        if (body == null || body.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(body);
    }
}
