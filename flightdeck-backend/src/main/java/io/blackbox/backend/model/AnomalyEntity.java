package io.blackbox.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "anomalies", indexes = {
        @Index(name = "idx_anomalies_service_name", columnList = "service_name"),
        @Index(name = "idx_anomalies_timestamp", columnList = "timestamp"),
        @Index(name = "idx_anomalies_incident_id", columnList = "incident_id")
})
public class AnomalyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anomaly_type", nullable = false, length = 64)
    private String anomalyType; // REDIS_LATENCY_ANOMALY, ERROR_RATE_ANOMALY, DB_TIMEOUT_ANOMALY

    @Column(name = "metric", nullable = false, length = 64)
    private String metric;

    @Column(name = "observed_value", nullable = false)
    private Double observedValue;

    @Column(name = "baseline_value", nullable = false)
    private Double baselineValue;

    @Column(name = "deviation", nullable = false)
    private Double deviation;

    @Column(name = "timestamp", nullable = false)
    private Long timestamp;

    @Column(name = "service_name", nullable = false, length = 128)
    private String serviceName;

    @Column(name = "severity", nullable = false, length = 32)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "incident_id", length = 64)
    private String incidentId;

    public AnomalyEntity() {}

    public AnomalyEntity(String anomalyType, String metric, Double observedValue, Double baselineValue, Double deviation, Long timestamp, String serviceName, String severity, String incidentId) {
        this.anomalyType = anomalyType;
        this.metric = metric;
        this.observedValue = observedValue;
        this.baselineValue = baselineValue;
        this.deviation = deviation;
        this.timestamp = timestamp;
        this.serviceName = serviceName;
        this.severity = severity;
        this.incidentId = incidentId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public Double getObservedValue() { return observedValue; }
    public void setObservedValue(Double observedValue) { this.observedValue = observedValue; }

    public Double getBaselineValue() { return baselineValue; }
    public void setBaselineValue(Double baselineValue) { this.baselineValue = baselineValue; }

    public Double getDeviation() { return deviation; }
    public void setDeviation(Double deviation) { this.deviation = deviation; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
}
