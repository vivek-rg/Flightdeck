package io.blackbox.backend.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.blackbox.backend.dto.NormalizedSpanDto;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TelemetryKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelemetryKafkaConsumer.class);

    private final TelemetryProcessorService telemetryProcessorService;
    private final ObjectMapper objectMapper;

    public TelemetryKafkaConsumer(TelemetryProcessorService telemetryProcessorService, ObjectMapper objectMapper) {
        this.telemetryProcessorService = telemetryProcessorService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${blackbox.kafka.telemetry-topic:blackbox.telemetry}",
            groupId = "blackbox-telemetry-group",
            autoStartup = "${spring.kafka.consumer.auto-startup:true}"
    )
    public void consumeTelemetry(ConsumerRecord<String, String> record) {
        String payload = record.value();
        try {
            if (payload.trim().startsWith("[")) {
                List<NormalizedSpanDto> spans = objectMapper.readValue(payload, new TypeReference<List<NormalizedSpanDto>>() {});
                telemetryProcessorService.processBatch(spans);
            } else {
                NormalizedSpanDto span = objectMapper.readValue(payload, NormalizedSpanDto.class);
                telemetryProcessorService.processNormalizedSpan(span);
            }
        } catch (Exception ex) {
            log.error("Failed to parse incoming telemetry record: {}", ex.getMessage());
        }
    }
}
