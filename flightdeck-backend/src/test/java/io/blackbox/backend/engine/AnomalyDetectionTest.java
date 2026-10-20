package io.blackbox.backend.engine;

import io.blackbox.backend.model.AnomalyEntity;
import io.blackbox.backend.model.SpanEntity;
import io.blackbox.backend.repository.AnomalyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionTest {

    @Mock
    private AnomalyRepository anomalyRepository;

    private AnomalyDetectionEngine anomalyEngine;

    @BeforeEach
    void setUp() {
        anomalyEngine = new AnomalyDetectionEngine(anomalyRepository);
    }

    @Test
    @DisplayName("Should detect REDIS_LATENCY_ANOMALY when observed latency spikes from baseline 8ms to 480ms")
    void testRedisLatencySpikeDetection() {
        SpanEntity normalSpan = new SpanEntity();
        normalSpan.setSpanId("sp-norm");
        normalSpan.setTraceId("tr-norm");
        normalSpan.setServiceName("payment-service");
        normalSpan.setOperationName("REDIS GET payment_lock:1");
        normalSpan.setDbSystem("redis");
        normalSpan.setDurationMs(8L);
        normalSpan.setStartTime(1700000000000L);
        normalSpan.setError(false);

        // Train baseline
        for (int i = 0; i < 5; i++) {
            anomalyEngine.analyzeSpan(normalSpan);
        }

        // Now simulate the latency spike
        SpanEntity spikeSpan = new SpanEntity();
        spikeSpan.setSpanId("sp-spike");
        spikeSpan.setTraceId("tr-spike");
        spikeSpan.setServiceName("payment-service");
        spikeSpan.setOperationName("REDIS GET payment_lock:2");
        spikeSpan.setDbSystem("redis");
        spikeSpan.setDurationMs(480L);
        spikeSpan.setStartTime(1700000001000L);
        spikeSpan.setError(false);

        List<AnomalyEntity> detected = anomalyEngine.analyzeSpan(spikeSpan);

        assertFalse(detected.isEmpty(), "Anomaly should be detected for 480ms Redis latency");
        AnomalyEntity anomaly = detected.get(0);
        assertEquals("REDIS_LATENCY_ANOMALY", anomaly.getAnomalyType());
        assertEquals(480.0, anomaly.getObservedValue());
        assertEquals("payment-service", anomaly.getServiceName());
        assertEquals("CRITICAL", anomaly.getSeverity());
        assertTrue(anomaly.getDeviation() >= 10.0, "Deviation should be >= 10x baseline");

        verify(anomalyRepository, atLeastOnce()).saveAll(any());
    }
}
