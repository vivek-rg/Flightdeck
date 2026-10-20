package io.blackbox.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.blackbox.backend.dto.NormalizedSpanDto;
import io.blackbox.backend.engine.TelemetryProcessorService;
import io.blackbox.backend.model.SpanEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryIngestController {

    private final TelemetryProcessorService telemetryProcessorService;
    private final ObjectMapper objectMapper;

    public TelemetryIngestController(TelemetryProcessorService telemetryProcessorService, ObjectMapper objectMapper) {
        this.telemetryProcessorService = telemetryProcessorService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/spans")
    public ResponseEntity<?> ingestSpans(@RequestBody String rawPayload) {
        try {
            if (rawPayload.trim().startsWith("[")) {
                List<NormalizedSpanDto> list = objectMapper.readValue(rawPayload, new TypeReference<List<NormalizedSpanDto>>() {});
                List<SpanEntity> processed = telemetryProcessorService.processBatch(list);
                return ResponseEntity.ok(Map.of("status", "SUCCESS", "processedCount", processed.size()));
            } else {
                NormalizedSpanDto single = objectMapper.readValue(rawPayload, NormalizedSpanDto.class);
                SpanEntity entity = telemetryProcessorService.processNormalizedSpan(single);
                return ResponseEntity.ok(Map.of("status", "SUCCESS", "spanId", entity.getSpanId()));
            }
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
