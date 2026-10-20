package io.blackbox.sample.common.failure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dev/failures")
@ConditionalOnProperty(name = "blackbox.dev-mode.enabled", havingValue = "true", matchIfMissing = true)
public class FailureInjectionController {

    private static final Logger log = LoggerFactory.getLogger(FailureInjectionController.class);
    private final FailureInjectionRegistry registry;

    public FailureInjectionController(FailureInjectionRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getFailures() {
        return ResponseEntity.ok(registry.getStatus());
    }

    @PostMapping("/redis-latency")
    public ResponseEntity<Map<String, Object>> injectRedisLatency(@RequestParam(name = "latencyMs", defaultValue = "480") Long latencyMs) {
        log.warn("INJECTED FAILURE: Redis latency spike configured to {}ms", latencyMs);
        registry.setRedisLatency(latencyMs);
        return ResponseEntity.ok(Map.of("message", "Redis latency set to " + latencyMs + "ms", "status", registry.getStatus()));
    }

    @PostMapping("/redis-unavailable")
    public ResponseEntity<Map<String, Object>> injectRedisUnavailable(@RequestParam(name = "unavailable", defaultValue = "true") Boolean unavailable) {
        log.warn("INJECTED FAILURE: Redis unavailable configured to {}", unavailable);
        registry.setRedisUnavailable(unavailable);
        return ResponseEntity.ok(Map.of("message", "Redis unavailable set to " + unavailable, "status", registry.getStatus()));
    }

    @PostMapping("/payment-timeout")
    public ResponseEntity<Map<String, Object>> injectPaymentTimeout(
            @RequestParam(name = "timeout", defaultValue = "true") Boolean timeout,
            @RequestParam(name = "delayMs", defaultValue = "2000") Long delayMs,
            @RequestParam(name = "failuresCount", defaultValue = "1") Integer failuresCount) {
        log.warn("INJECTED FAILURE: Payment timeout configured: timeout={}, delayMs={}, failuresCount={}", timeout, delayMs, failuresCount);
        registry.setPaymentTimeout(timeout);
        registry.setPaymentDelay(delayMs);
        registry.setPaymentFailuresCount(failuresCount);
        return ResponseEntity.ok(Map.of("message", "Payment timeout configured", "status", registry.getStatus()));
    }

    @PostMapping("/kafka-delay")
    public ResponseEntity<Map<String, Object>> injectKafkaDelay(@RequestParam(name = "delayMs", defaultValue = "3000") Long delayMs) {
        log.warn("INJECTED FAILURE: Kafka consumer delay configured to {}ms", delayMs);
        registry.setKafkaConsumerDelay(delayMs);
        return ResponseEntity.ok(Map.of("message", "Kafka consumer delay set to " + delayMs + "ms", "status", registry.getStatus()));
    }

    @PostMapping("/kafka-duplicate")
    public ResponseEntity<Map<String, Object>> injectKafkaDuplicate(@RequestParam(name = "duplicate", defaultValue = "true") Boolean duplicate) {
        log.warn("INJECTED FAILURE: Kafka duplicate event configured to {}", duplicate);
        registry.setKafkaDuplicateEvent(duplicate);
        return ResponseEntity.ok(Map.of("message", "Kafka duplicate configured to " + duplicate, "status", registry.getStatus()));
    }

    @PostMapping("/db-unavailable")
    public ResponseEntity<Map<String, Object>> injectDbUnavailable(@RequestParam(name = "unavailable", defaultValue = "true") Boolean unavailable) {
        log.warn("INJECTED FAILURE: Database unavailable configured to {}", unavailable);
        registry.setDatabaseUnavailable(unavailable);
        return ResponseEntity.ok(Map.of("message", "DB unavailable set to " + unavailable, "status", registry.getStatus()));
    }

    @PostMapping("/service-unavailable")
    public ResponseEntity<Map<String, Object>> injectServiceUnavailable(@RequestParam(name = "unavailable", defaultValue = "true") Boolean unavailable) {
        log.warn("INJECTED FAILURE: Service unavailable configured to {}", unavailable);
        registry.setServiceUnavailable(unavailable);
        return ResponseEntity.ok(Map.of("message", "Service unavailable set to " + unavailable, "status", registry.getStatus()));
    }

    @PostMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearFailures() {
        log.info("CLEARED all failure injections.");
        registry.clearAll();
        return ResponseEntity.ok(Map.of("message", "All injected failures cleared", "status", registry.getStatus()));
    }
}
