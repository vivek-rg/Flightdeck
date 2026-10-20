package io.blackbox.backend.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.blackbox.backend.model.*;
import io.blackbox.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RootCauseAnalysisEngineTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentEventRepository eventRepository;

    @Mock
    private AnomalyRepository anomalyRepository;

    @Mock
    private DependencyRepository dependencyRepository;

    @Mock
    private SpanRepository spanRepository;

    @Mock
    private RootCauseCandidateRepository candidateRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private RootCauseAnalysisEngine rcaEngine;

    @BeforeEach
    void setUp() {
        rcaEngine = new RootCauseAnalysisEngine(
                incidentRepository,
                eventRepository,
                anomalyRepository,
                dependencyRepository,
                spanRepository,
                candidateRepository,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should deterministically identify Redis latency spike as primary root cause with high confidence")
    void testRedisLatencyRootCauseScoring() {
        String incidentId = "INC-TEST-001";
        long t0 = 1700000000000L;

        IncidentEntity incident = new IncidentEntity(incidentId, t0, "HIGH", "ACTIVE", "payment-service", "Payment failure incident");
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        // Incident timeline events
        IncidentEventEntity e1 = new IncidentEventEntity(t0, "order-service", "ORDER_CREATED", "Order initiated", "tr-1", "sp-1", false);
        IncidentEventEntity e2 = new IncidentEventEntity(t0 + 100, "payment-service", "PAYMENT_REQUESTED", "Payment requested", "tr-1", "sp-2", false);
        IncidentEventEntity e3 = new IncidentEventEntity(t0 + 150, "payment-service", "REDIS_LATENCY_SPIKE", "Redis latency 480ms", "tr-1", "sp-3", true);
        IncidentEventEntity e4 = new IncidentEventEntity(t0 + 2150, "payment-service", "PAYMENT_TIMEOUT", "Payment gateway timeout", "tr-1", "sp-4", false);
        IncidentEventEntity e5 = new IncidentEventEntity(t0 + 2350, "order-service", "PAYMENT_RETRY", "Payment retry attempt 2", "tr-1", "sp-5", false);
        IncidentEventEntity e6 = new IncidentEventEntity(t0 + 2400, "order-service", "ORDER_FAILED", "Order processing failed", "tr-1", "sp-6", false);

        when(eventRepository.findByIncidentIncidentIdOrderByTimestampAsc(incidentId)).thenReturn(List.of(e1, e2, e3, e4, e5, e6));

        // Redis latency anomaly (480ms vs 8ms normal = 60x deviation)
        AnomalyEntity anomaly = new AnomalyEntity("REDIS_LATENCY_ANOMALY", "LATENCY_MS", 480.0, 8.0, 60.0, t0 + 150, "payment-service", "CRITICAL", incidentId);
        when(anomalyRepository.findByIncidentId(incidentId)).thenReturn(List.of(anomaly));

        // Discovered dependencies
        DependencyEntity dep1 = new DependencyEntity("order-service", "payment-service", "HTTP");
        DependencyEntity dep2 = new DependencyEntity("payment-service", "redis", "REDIS");
        when(dependencyRepository.findAll()).thenReturn(List.of(dep1, dep2));

        when(candidateRepository.save(any(RootCauseCandidateEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Execute deterministic RCA
        List<RootCauseCandidateEntity> candidates = rcaEngine.analyzeIncident(incidentId);

        assertNotNull(candidates);
        assertFalse(candidates.isEmpty());

        // Verify Top Root Cause
        RootCauseCandidateEntity topCandidate = candidates.get(0);
        assertEquals("Redis Latency Spike", topCandidate.getCandidateName());
        assertTrue(topCandidate.isPrimaryRootCause(), "Redis Latency Spike should be marked as primary root cause");
        assertTrue(topCandidate.getConfidence() >= 0.80, "Confidence should be at least 80%");
        assertTrue(topCandidate.getScore() >= 80.0, "Score should be >= 80");

        // Verify scoring components breakdown
        assertTrue(topCandidate.getTemporalScore() >= 18.0, "Temporal score should be high due to early occurrence");
        assertTrue(topCandidate.getDependencyScore() >= 15.0, "Dependency score should be high because Redis is upstream leaf");
        assertTrue(topCandidate.getAnomalyScore() >= 18.0, "Anomaly score should be high for 60x latency jump");
        assertTrue(topCandidate.getPropagationScore() >= 10.0, "Propagation score should reflect error bubbling up");
        assertTrue(topCandidate.getRetryScore() >= 8.0, "Retry score should reflect triggered downstream retries");

        // Verify symptom vs root cause distinction: Order failure should rank lower than Redis latency spike
        boolean orderRankedBelow = candidates.stream()
                .filter(c -> c.getCandidateName().contains("Order Service"))
                .allMatch(c -> c.getScore() < topCandidate.getScore());
        assertTrue(orderRankedBelow, "Order Service Processing Failure must be identified as a downstream SYMPTOM, not the root cause!");
    }
}
