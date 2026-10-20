package io.blackbox.backend.engine;

import io.blackbox.backend.model.*;
import io.blackbox.backend.repository.IncidentEventRepository;
import io.blackbox.backend.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IncidentDetectionEngine {

    private static final Logger log = LoggerFactory.getLogger(IncidentDetectionEngine.class);
    private static final long INCIDENT_GROUPING_WINDOW_MS = 45_000; // 45 seconds

    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final RootCauseAnalysisEngine rootCauseEngine;

    // In-memory pointer to current active incident ID
    private volatile String currentActiveIncidentId = null;
    private volatile long lastEventTime = 0;

    public IncidentDetectionEngine(IncidentRepository incidentRepository,
                                   IncidentEventRepository incidentEventRepository,
                                   @Lazy RootCauseAnalysisEngine rootCauseEngine) {
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.rootCauseEngine = rootCauseEngine;
    }

    @Transactional
    public synchronized IncidentEntity processFailureOrAnomaly(SpanEntity span, List<AnomalyEntity> anomalies) {
        long now = span.getStartTime() != null ? span.getStartTime() : System.currentTimeMillis();

        // 1. Group with current active incident or create a new incident
        IncidentEntity incident;
        if (currentActiveIncidentId != null && (now - lastEventTime) <= INCIDENT_GROUPING_WINDOW_MS) {
            incident = incidentRepository.findById(currentActiveIncidentId).orElse(null);
            if (incident == null || "RESOLVED".equals(incident.getStatus())) {
                incident = createNewIncident(span, anomalies, now);
            }
        } else {
            incident = createNewIncident(span, anomalies, now);
        }

        currentActiveIncidentId = incident.getIncidentId();
        lastEventTime = now;

        // 2. Classify event and add to incident timeline
        String eventType = determineEventType(span, anomalies);
        String description = determineDescription(span, eventType);
        boolean isRootCauseCandidatePoint = (eventType.contains("LATENCY_SPIKE") || eventType.contains("REDIS") || eventType.contains("DATABASE_UNAVAILABLE"));

        IncidentEventEntity event = new IncidentEventEntity(
                now,
                span.getServiceName(),
                eventType,
                description,
                span.getTraceId(),
                span.getSpanId(),
                isRootCauseCandidatePoint
        );
        event.setIncident(incident);
        incidentEventRepository.save(event);

        // Link anomalies to this incident
        for (AnomalyEntity a : anomalies) {
            a.setIncidentId(incident.getIncidentId());
        }

        // Update incident severity if multiple services affected
        if (span.isError() && "MEDIUM".equals(incident.getSeverity())) {
            incident.setSeverity("HIGH");
        }
        if (eventType.contains("REDIS") || eventType.contains("TIMEOUT") || eventType.contains("CASCADING")) {
            incident.setSeverity("CRITICAL");
        }
        incident.setEndTime(now);
        incidentRepository.save(incident);

        // 3. Incrementally trigger Root Cause Analysis to maintain latest RCA evidence
        try {
            rootCauseEngine.analyzeIncident(incident.getIncidentId());
        } catch (Exception ex) {
            log.warn("Root cause analysis calculation deferred: {}", ex.getMessage());
        }

        return incident;
    }

    private IncidentEntity createNewIncident(SpanEntity span, List<AnomalyEntity> anomalies, long timestamp) {
        String incidentId = "INC-" + Instant.ofEpochMilli(timestamp).toString().replace("-", "").replace(":", "").substring(0, 15)
                + "-" + (int)(Math.random() * 900 + 100);

        String initialService = span.getServiceName();
        String description = "Incident detected on service [" + initialService + "]: "
                + (span.getStatusMessage() != null ? span.getStatusMessage() : span.getOperationName());

        log.warn("NEW INCIDENT CREATED: [{}] Triggered by service [{}]", incidentId, initialService);

        IncidentEntity entity = new IncidentEntity(
                incidentId,
                timestamp,
                "HIGH",
                "ACTIVE",
                initialService,
                description
        );
        return incidentRepository.save(entity);
    }

    private String determineEventType(SpanEntity span, List<AnomalyEntity> anomalies) {
        for (AnomalyEntity a : anomalies) {
            if ("REDIS_LATENCY_ANOMALY".equals(a.getAnomalyType())) return "REDIS_LATENCY_SPIKE";
            if ("PAYMENT_TIMEOUT_ANOMALY".equals(a.getAnomalyType())) return "PAYMENT_TIMEOUT";
            if ("DATABASE_ERROR_ANOMALY".equals(a.getAnomalyType())) return "DATABASE_FAILURE";
            if ("KAFKA_DELAY_ANOMALY".equals(a.getAnomalyType())) return "KAFKA_DELAY";
        }

        if (span.isError()) {
            String msg = (span.getStatusMessage() + " " + span.getAttributesJson()).toLowerCase();
            if (msg.contains("timeout") || span.getDurationMs() > 2000) return "PAYMENT_TIMEOUT";
            if (msg.contains("retry")) return "PAYMENT_RETRY";
            if (msg.contains("duplicate")) return "DUPLICATE_KAFKA_EVENT";
            if (msg.contains("stock") || msg.contains("inventory")) return "INVENTORY_FAILURE";
            if (msg.contains("order")) return "ORDER_FAILED";
            return "SERVICE_ERROR";
        }

        if ("orders.created".equalsIgnoreCase(span.getOperationName()) || span.getOperationName().contains("POST /orders")) {
            return "ORDER_CREATED";
        }
        if (span.getOperationName().contains("/payments")) {
            return "PAYMENT_REQUESTED";
        }

        return "SERVICE_OPERATION";
    }

    private String determineDescription(SpanEntity span, String eventType) {
        return switch (eventType) {
            case "ORDER_CREATED" -> "Order creation request initiated on " + span.getServiceName();
            case "PAYMENT_REQUESTED" -> "Payment authorization requested for order";
            case "REDIS_LATENCY_SPIKE" -> "Redis latency spiked to " + span.getDurationMs() + "ms on " + span.getServiceName();
            case "PAYMENT_TIMEOUT" -> "Payment operation timed out after " + span.getDurationMs() + "ms";
            case "PAYMENT_RETRY" -> "Downstream retry executed for payment failure";
            case "DUPLICATE_KAFKA_EVENT" -> "Duplicate event observed on Kafka orders topic";
            case "INVENTORY_FAILURE" -> "Inventory stock reservation failed downstream: " + span.getStatusMessage();
            case "ORDER_FAILED" -> "Order failed due to upstream dependency failure";
            case "DATABASE_FAILURE" -> "Database query failed: " + span.getStatusMessage();
            default -> "Observed operation: " + span.getOperationName() + " (" + span.getDurationMs() + "ms)";
        };
    }

    public List<IncidentEntity> getAllIncidents() {
        return incidentRepository.findAllOrderByStartTimeDesc();
    }

    public List<IncidentEntity> getActiveIncidents() {
        return incidentRepository.findActiveIncidents();
    }

    public IncidentEntity getIncident(String incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new NoSuchElementException("Incident not found: " + incidentId));
    }

    public List<IncidentEventEntity> getTimeline(String incidentId) {
        return incidentEventRepository.findByIncidentIncidentIdOrderByTimestampAsc(incidentId);
    }
}
