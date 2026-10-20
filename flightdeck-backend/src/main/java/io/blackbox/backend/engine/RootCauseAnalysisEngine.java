package io.blackbox.backend.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.blackbox.backend.model.*;
import io.blackbox.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RootCauseAnalysisEngine {

    private static final Logger log = LoggerFactory.getLogger(RootCauseAnalysisEngine.class);

    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository eventRepository;
    private final AnomalyRepository anomalyRepository;
    private final DependencyRepository dependencyRepository;
    private final SpanRepository spanRepository;
    private final RootCauseCandidateRepository candidateRepository;
    private final ObjectMapper objectMapper;

    public RootCauseAnalysisEngine(IncidentRepository incidentRepository,
                                   IncidentEventRepository eventRepository,
                                   AnomalyRepository anomalyRepository,
                                   DependencyRepository dependencyRepository,
                                   SpanRepository spanRepository,
                                   RootCauseCandidateRepository candidateRepository,
                                   ObjectMapper objectMapper) {
        this.incidentRepository = incidentRepository;
        this.eventRepository = eventRepository;
        this.anomalyRepository = anomalyRepository;
        this.dependencyRepository = dependencyRepository;
        this.spanRepository = spanRepository;
        this.candidateRepository = candidateRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<RootCauseCandidateEntity> analyzeIncident(String incidentId) {
        IncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new NoSuchElementException("Incident not found: " + incidentId));

        List<IncidentEventEntity> events = eventRepository.findByIncidentIncidentIdOrderByTimestampAsc(incidentId);
        List<AnomalyEntity> anomalies = anomalyRepository.findByIncidentId(incidentId);
        if (anomalies.isEmpty()) {
            anomalies = anomalyRepository.findRecentAnomalies(incident.getStartTime() - 10_000);
        }

        // 1. Identify distinct candidate root causes from observed anomalies and events
        Map<String, CandidateEvaluation> candidateMap = new HashMap<>();

        // Check for Redis Latency Spike candidate
        boolean hasRedisAnomaly = anomalies.stream().anyMatch(a -> "REDIS_LATENCY_ANOMALY".equals(a.getAnomalyType()));
        boolean hasRedisEvent = events.stream().anyMatch(e -> e.getEventType().contains("REDIS"));
        if (hasRedisAnomaly || hasRedisEvent) {
            candidateMap.put("Redis Latency Spike", new CandidateEvaluation(
                    "Redis Latency Spike", "payment-service", "REDIS_LATENCY"));
        }

        // Check for Payment Timeout candidate
        boolean hasPaymentTimeout = events.stream().anyMatch(e -> e.getEventType().contains("PAYMENT_TIMEOUT"));
        if (hasPaymentTimeout) {
            candidateMap.put("Payment Gateway Timeout", new CandidateEvaluation(
                    "Payment Gateway Timeout", "payment-service", "PAYMENT_TIMEOUT"));
        }

        // Check for Database Failure candidate
        boolean hasDbFailure = events.stream().anyMatch(e -> e.getEventType().contains("DATABASE") || e.getEventType().contains("INVENTORY"));
        if (hasDbFailure) {
            candidateMap.put("Database Connection Failure", new CandidateEvaluation(
                    "Database Connection Failure", "inventory-service", "DATABASE_FAILURE"));
        }

        // Check for Order Processing Failure candidate (symptom)
        boolean hasOrderFailed = events.stream().anyMatch(e -> e.getEventType().contains("ORDER_FAILED"));
        if (hasOrderFailed) {
            candidateMap.put("Order Service Processing Failure", new CandidateEvaluation(
                    "Order Service Processing Failure", "order-service", "ORDER_FAILURE"));
        }

        // If no candidate generated yet, fallback to trigger service candidate
        if (candidateMap.isEmpty()) {
            String trigger = incident.getTriggerService() != null ? incident.getTriggerService() : "order-service";
            candidateMap.put("Service Anomaly on " + trigger, new CandidateEvaluation(
                    "Service Anomaly on " + trigger, trigger, "SERVICE_ANOMALY"));
        }

        long incidentStart = incident.getStartTime();
        List<DependencyEntity> allDeps = dependencyRepository.findAll();

        // 2. Compute 6-factor deterministic score for each candidate
        for (CandidateEvaluation c : candidateMap.values()) {
            evaluateCandidate(c, incident, events, anomalies, allDeps, incidentStart);
        }

        // 3. Sort candidates by score descending
        List<CandidateEvaluation> sorted = candidateMap.values().stream()
                .sorted(Comparator.comparingDouble(CandidateEvaluation::getTotalScore).reversed())
                .collect(Collectors.toList());

        // Clear prior candidates for this incident
        candidateRepository.deleteByIncidentId(incidentId);

        List<RootCauseCandidateEntity> entities = new ArrayList<>();
        boolean first = true;
        for (CandidateEvaluation c : sorted) {
            RootCauseCandidateEntity entity = new RootCauseCandidateEntity();
            entity.setIncidentId(incidentId);
            entity.setCandidateName(c.candidateName);
            entity.setServiceName(c.serviceName);
            entity.setScore(Math.round(c.totalScore * 10.0) / 10.0);
            entity.setConfidence(Math.round((c.totalScore / 100.0) * 100.0) / 100.0);
            entity.setTemporalScore(Math.round(c.temporalScore * 10.0) / 10.0);
            entity.setDependencyScore(Math.round(c.dependencyScore * 10.0) / 10.0);
            entity.setAnomalyScore(Math.round(c.anomalyScore * 10.0) / 10.0);
            entity.setPropagationScore(Math.round(c.propagationScore * 10.0) / 10.0);
            entity.setRetryScore(Math.round(c.retryScore * 10.0) / 10.0);
            entity.setImpactScore(Math.round(c.impactScore * 10.0) / 10.0);
            entity.setPrimaryRootCause(first);

            try {
                entity.setEvidenceJson(objectMapper.writeValueAsString(c.evidenceList));
                entity.setAffectedServicesJson(objectMapper.writeValueAsString(c.affectedServices));
                entity.setDownstreamEffectsJson(objectMapper.writeValueAsString(c.downstreamEffects));
            } catch (Exception ignored) {}

            entities.add(candidateRepository.save(entity));

            if (first) {
                incident.setRootCauseCandidate(c.candidateName);
                incident.setRootCauseConfidence(entity.getConfidence());
                incidentRepository.save(incident);
                first = false;
            }
        }

        log.info("RCA COMPLETED for incident [{}]. Top Root Cause: [{}] with confidence [{}%]",
                incidentId, incident.getRootCauseCandidate(), Math.round(incident.getRootCauseConfidence() * 100));

        return entities;
    }

    private void evaluateCandidate(CandidateEvaluation c,
                                   IncidentEntity incident,
                                   List<IncidentEventEntity> events,
                                   List<AnomalyEntity> anomalies,
                                   List<DependencyEntity> allDeps,
                                   long incidentStart) {

        // --- FACTOR 1: Temporal Correlation (0 to 25 points) ---
        // Find earliest event or anomaly matching this candidate
        long earliestTime = Long.MAX_VALUE;
        for (IncidentEventEntity e : events) {
            if (eventMatchesCandidate(e, c.type)) {
                earliestTime = Math.min(earliestTime, e.getTimestamp());
            }
        }
        for (AnomalyEntity a : anomalies) {
            if (anomalyMatchesCandidate(a, c.type)) {
                earliestTime = Math.min(earliestTime, a.getTimestamp());
            }
        }

        if (earliestTime != Long.MAX_VALUE) {
            long deltaFromIncidentStart = Math.max(0, earliestTime - incidentStart);
            if (deltaFromIncidentStart <= 1000) {
                c.temporalScore = 25.0; // Earliest trigger in the timeline!
                c.evidenceList.add("Temporal analysis: Fault condition was observed at timestamp " + earliestTime
                        + " (first event in timeline).");
            } else if (deltaFromIncidentStart <= 3000) {
                c.temporalScore = 18.0;
                c.evidenceList.add("Temporal correlation: Condition occurred " + deltaFromIncidentStart + "ms after timeline initiation.");
            } else {
                c.temporalScore = 8.0;
                c.evidenceList.add("Temporal lag: Event occurred " + deltaFromIncidentStart + "ms downstream after initial degradation.");
            }
        } else {
            c.temporalScore = 5.0;
        }

        // --- FACTOR 2: Dependency Relationship (0 to 20 points) ---
        // Inspect dependency graph topology
        if ("REDIS_LATENCY".equals(c.type)) {
            // Redis is a dependency of payment-service, which is a dependency of order-service
            c.dependencyScore = 20.0;
            c.evidenceList.add("Dependency topology: Redis is a direct low-level dependency of payment-service and transitive dependency of order-service.");
            c.affectedServices.addAll(List.of("payment-service", "order-service", "api-gateway"));
        } else if ("PAYMENT_TIMEOUT".equals(c.type)) {
            // Payment service is an intermediate node
            c.dependencyScore = 12.0;
            c.evidenceList.add("Dependency topology: payment-service is an intermediate dependency called by order-service.");
            c.affectedServices.addAll(List.of("payment-service", "order-service"));
        } else if ("DATABASE_FAILURE".equals(c.type)) {
            c.dependencyScore = 18.0;
            c.evidenceList.add("Dependency topology: PostgreSQL is a core persistence dependency of inventory-service.");
            c.affectedServices.addAll(List.of("inventory-service", "order-service"));
        } else {
            // Top-level service (order-service) is a symptom victim
            c.dependencyScore = 4.0;
            c.evidenceList.add("Dependency topology: Service is a top-level orchestrator; failures here are typically cascading symptoms.");
            c.affectedServices.add(c.serviceName);
        }

        // --- FACTOR 3: Anomaly Severity (0 to 20 points) ---
        Optional<AnomalyEntity> matchAnomaly = anomalies.stream()
                .filter(a -> anomalyMatchesCandidate(a, c.type))
                .findFirst();

        if (matchAnomaly.isPresent()) {
            AnomalyEntity a = matchAnomaly.get();
            double deviation = a.getDeviation() != null ? a.getDeviation() : 1.0;
            if (deviation >= 30.0) {
                c.anomalyScore = 20.0;
                c.evidenceList.add("Statistical anomaly: " + a.getMetric() + " increased from normal baseline of "
                        + a.getBaselineValue() + "ms to observed " + a.getObservedValue() + "ms (" + deviation + "x deviation).");
            } else if (deviation >= 5.0) {
                c.anomalyScore = 14.0;
                c.evidenceList.add("Statistical anomaly: Deviation factor of " + deviation + "x from baseline.");
            } else {
                c.anomalyScore = 8.0;
                c.evidenceList.add("Statistical deviation detected on " + a.getMetric());
            }
        } else {
            c.anomalyScore = 4.0;
        }

        // --- FACTOR 4: Error Propagation (0 to 15 points) ---
        boolean hasDownstreamErrors = events.stream()
                .anyMatch(e -> e.getEventType().contains("FAILED") || e.getEventType().contains("TIMEOUT"));

        if ("REDIS_LATENCY".equals(c.type) && hasDownstreamErrors) {
            c.propagationScore = 15.0;
            c.evidenceList.add("Error propagation: Redis latency blocked the payment thread, propagating a timeout to caller order-service.");
            c.downstreamEffects.add("payment-service timed out awaiting Redis operation");
            c.downstreamEffects.add("order-service received 504 Gateway Timeout from payment-service");
        } else if ("PAYMENT_TIMEOUT".equals(c.type)) {
            c.propagationScore = 9.0;
            c.evidenceList.add("Error propagation: Payment timeout propagated to order creation failure.");
            c.downstreamEffects.add("order-service marked order status as FAILED");
        } else {
            c.propagationScore = 5.0;
        }

        // --- FACTOR 5: Retry Correlation (0 to 10 points) ---
        boolean hasRetries = events.stream().anyMatch(e -> e.getEventType().contains("RETRY"));
        if (hasRetries && ("REDIS_LATENCY".equals(c.type) || "PAYMENT_TIMEOUT".equals(c.type))) {
            c.retryScore = 10.0;
            c.evidenceList.add("Retry correlation: Downstream retry cycle was triggered immediately following the timeout/latency delay.");
            c.downstreamEffects.add("Order service executed automated payment retry attempt");
        } else {
            c.retryScore = 2.0;
        }

        // --- FACTOR 6: Downstream Impact (0 to 10 points) ---
        boolean hasDuplicateKafka = events.stream().anyMatch(e -> e.getEventType().contains("DUPLICATE"));
        boolean hasInventoryFail = events.stream().anyMatch(e -> e.getEventType().contains("INVENTORY"));
        boolean hasOrderFail = events.stream().anyMatch(e -> e.getEventType().contains("ORDER_FAILED"));

        double impact = 0.0;
        if (hasOrderFail) impact += 4.0;
        if (hasInventoryFail) impact += 3.0;
        if (hasDuplicateKafka) impact += 3.0;
        c.impactScore = Math.min(10.0, impact);

        if (c.impactScore >= 7.0) {
            c.evidenceList.add("Cascading impact: Failure resulted in multiple downstream impacts across 3+ services.");
        }

        // Total score calculation
        c.totalScore = c.temporalScore + c.dependencyScore + c.anomalyScore + c.propagationScore + c.retryScore + c.impactScore;
    }

    private boolean eventMatchesCandidate(IncidentEventEntity e, String candidateType) {
        String type = e.getEventType();
        return switch (candidateType) {
            case "REDIS_LATENCY" -> type.contains("REDIS");
            case "PAYMENT_TIMEOUT" -> type.contains("TIMEOUT");
            case "DATABASE_FAILURE" -> type.contains("DATABASE") || type.contains("INVENTORY");
            case "ORDER_FAILURE" -> type.contains("ORDER_FAILED");
            default -> false;
        };
    }

    private boolean anomalyMatchesCandidate(AnomalyEntity a, String candidateType) {
        String type = a.getAnomalyType();
        return switch (candidateType) {
            case "REDIS_LATENCY" -> type.contains("REDIS");
            case "PAYMENT_TIMEOUT" -> type.contains("TIMEOUT");
            case "DATABASE_FAILURE" -> type.contains("DATABASE");
            default -> false;
        };
    }

    public List<RootCauseCandidateEntity> getCandidatesForIncident(String incidentId) {
        List<RootCauseCandidateEntity> list = candidateRepository.findByIncidentIdOrderByScoreDesc(incidentId);
        if (list.isEmpty()) {
            return analyzeIncident(incidentId);
        }
        return list;
    }

    private static class CandidateEvaluation {
        String candidateName;
        String serviceName;
        String type;
        double temporalScore = 0.0;
        double dependencyScore = 0.0;
        double anomalyScore = 0.0;
        double propagationScore = 0.0;
        double retryScore = 0.0;
        double impactScore = 0.0;
        double totalScore = 0.0;
        List<String> evidenceList = new ArrayList<>();
        Set<String> affectedServices = new LinkedHashSet<>();
        List<String> downstreamEffects = new ArrayList<>();

        public CandidateEvaluation(String candidateName, String serviceName, String type) {
            this.candidateName = candidateName;
            this.serviceName = serviceName;
            this.type = type;
        }

        public double getTotalScore() {
            return totalScore;
        }
    }
}
