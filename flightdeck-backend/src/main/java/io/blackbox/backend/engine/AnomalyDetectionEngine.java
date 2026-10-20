package io.blackbox.backend.engine;

import io.blackbox.backend.model.AnomalyEntity;
import io.blackbox.backend.model.SpanEntity;
import io.blackbox.backend.repository.AnomalyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnomalyDetectionEngine {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionEngine.class);

    private final AnomalyRepository anomalyRepository;

    // Rolling statistics per target key: (count, mean, M2 for variance calculation)
    private final Map<String, RollingStats> statsMap = new ConcurrentHashMap<>();

    public AnomalyDetectionEngine(AnomalyRepository anomalyRepository) {
        this.anomalyRepository = anomalyRepository;
    }

    @Transactional
    public List<AnomalyEntity> analyzeSpan(SpanEntity span) {
        List<AnomalyEntity> detected = new ArrayList<>();

        // 1. Redis Latency Analysis
        if ("redis".equalsIgnoreCase(span.getDbSystem()) || span.getOperationName().toLowerCase().contains("redis")) {
            String key = span.getServiceName() + ":redis:latency";
            RollingStats stats = statsMap.computeIfAbsent(key, k -> new RollingStats(8.0, 3.0)); // 8ms default baseline

            double observed = (double) span.getDurationMs();
            double baseline = stats.getMean();
            double stdDev = stats.getStdDev();

            // Detect significant latency spike (> 3 stddevs or > 10x baseline when duration > 100ms)
            if (observed > 100 && (observed > baseline + 3 * stdDev || observed > baseline * 5)) {
                double deviationRatio = Math.round((observed / Math.max(1.0, baseline)) * 10.0) / 10.0;
                String severity = observed > 300 ? "CRITICAL" : "HIGH";

                AnomalyEntity anomaly = new AnomalyEntity(
                        "REDIS_LATENCY_ANOMALY",
                        "LATENCY_MS",
                        observed,
                        baseline,
                        deviationRatio,
                        span.getStartTime(),
                        span.getServiceName(),
                        severity,
                        null
                );
                detected.add(anomaly);
                log.warn("ANOMALY DETECTED: Redis latency spike on service [{}]: observed={}ms, baseline={}ms ({}x deviation)",
                        span.getServiceName(), observed, baseline, deviationRatio);
            } else {
                stats.update(observed);
            }
        }

        // 2. Database (PostgreSQL) Latency / Failure Analysis
        if ("postgresql".equalsIgnoreCase(span.getDbSystem()) || span.getOperationName().toLowerCase().contains("postgres")) {
            String key = span.getServiceName() + ":postgres:latency";
            RollingStats stats = statsMap.computeIfAbsent(key, k -> new RollingStats(12.0, 4.0));

            double observed = (double) span.getDurationMs();
            double baseline = stats.getMean();

            if (span.isError()) {
                AnomalyEntity anomaly = new AnomalyEntity(
                        "DATABASE_ERROR_ANOMALY",
                        "ERROR_STATUS",
                        1.0,
                        0.0,
                        1.0,
                        span.getStartTime(),
                        span.getServiceName(),
                        "CRITICAL",
                        null
                );
                detected.add(anomaly);
            } else if (observed > 200 && observed > baseline * 5) {
                AnomalyEntity anomaly = new AnomalyEntity(
                        "DATABASE_LATENCY_ANOMALY",
                        "LATENCY_MS",
                        observed,
                        baseline,
                        Math.round((observed / baseline) * 10.0) / 10.0,
                        span.getStartTime(),
                        span.getServiceName(),
                        "HIGH",
                        null
                );
                detected.add(anomaly);
            } else {
                stats.update(observed);
            }
        }

        // 3. HTTP Timeout / Latency Spike Analysis on Services
        if ("SERVER".equalsIgnoreCase(span.getSpanKind()) || "CLIENT".equalsIgnoreCase(span.getSpanKind())) {
            String key = span.getServiceName() + ":http:latency";
            RollingStats stats = statsMap.computeIfAbsent(key, k -> new RollingStats(25.0, 8.0));

            double observed = (double) span.getDurationMs();
            double baseline = stats.getMean();

            if (observed > 1500) {
                String type = span.getServiceName().contains("payment") ? "PAYMENT_TIMEOUT_ANOMALY" : "SERVICE_TIMEOUT_ANOMALY";
                AnomalyEntity anomaly = new AnomalyEntity(
                        type,
                        "LATENCY_MS",
                        observed,
                        baseline,
                        Math.round((observed / Math.max(1.0, baseline)) * 10.0) / 10.0,
                        span.getStartTime(),
                        span.getServiceName(),
                        "CRITICAL",
                        null
                );
                detected.add(anomaly);
            } else if (!span.isError()) {
                stats.update(observed);
            }
        }

        // 4. Kafka Delay / Duplicate Analysis
        if ("kafka".equalsIgnoreCase(span.getMessagingSystem())) {
            if (span.getDurationMs() > 2000) {
                AnomalyEntity anomaly = new AnomalyEntity(
                        "KAFKA_DELAY_ANOMALY",
                        "PROCESSING_TIME_MS",
                        (double) span.getDurationMs(),
                        15.0,
                        Math.round((span.getDurationMs() / 15.0) * 10.0) / 10.0,
                        span.getStartTime(),
                        span.getServiceName(),
                        "HIGH",
                        null
                );
                detected.add(anomaly);
            }
        }

        // Save detected anomalies to database
        if (!detected.isEmpty()) {
            anomalyRepository.saveAll(detected);
        }

        return detected;
    }

    public List<AnomalyEntity> getRecentAnomalies(Long since) {
        return anomalyRepository.findRecentAnomalies(since != null ? since : System.currentTimeMillis() - 3600_000);
    }

    public List<AnomalyEntity> getAllAnomalies() {
        return anomalyRepository.findAllOrderByTimestampDesc();
    }

    // Helper class for Welford's algorithm online variance calculation
    public static class RollingStats {
        private long count;
        private double mean;
        private double m2;

        public RollingStats(double initialMean, double initialStdDev) {
            this.count = 20; // Prior weight
            this.mean = initialMean;
            this.m2 = (initialStdDev * initialStdDev) * 20;
        }

        public synchronized void update(double x) {
            count++;
            double delta = x - mean;
            mean += delta / count;
            double delta2 = x - mean;
            m2 += delta * delta2;
        }

        public synchronized double getMean() {
            return Math.round(mean * 10.0) / 10.0;
        }

        public synchronized double getStdDev() {
            if (count < 2) return 1.0;
            return Math.max(1.0, Math.round(Math.sqrt(m2 / (count - 1)) * 10.0) / 10.0);
        }
    }
}
