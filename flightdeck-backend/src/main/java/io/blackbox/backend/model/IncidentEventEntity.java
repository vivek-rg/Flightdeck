package io.blackbox.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "incident_events", indexes = {
        @Index(name = "idx_inc_events_incident_id", columnList = "incident_id"),
        @Index(name = "idx_inc_events_timestamp", columnList = "timestamp"),
        @Index(name = "idx_inc_events_trace_id", columnList = "trace_id")
})
public class IncidentEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    @JsonIgnore
    private IncidentEntity incident;

    @Column(name = "timestamp", nullable = false)
    private Long timestamp;

    @Column(name = "service_name", nullable = false, length = 128)
    private String serviceName;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "description", nullable = false, length = 512)
    private String description;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "span_id", length = 64)
    private String spanId;

    @Column(name = "is_root_cause_point")
    private boolean isRootCausePoint;

    public IncidentEventEntity() {}

    public IncidentEventEntity(Long timestamp, String serviceName, String eventType, String description, String traceId, String spanId, boolean isRootCausePoint) {
        this.timestamp = timestamp;
        this.serviceName = serviceName;
        this.eventType = eventType;
        this.description = description;
        this.traceId = traceId;
        this.spanId = spanId;
        this.isRootCausePoint = isRootCausePoint;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public IncidentEntity getIncident() { return incident; }
    public void setIncident(IncidentEntity incident) { this.incident = incident; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getSpanId() { return spanId; }
    public void setSpanId(String spanId) { this.spanId = spanId; }

    public boolean isRootCausePoint() { return isRootCausePoint; }
    public void setRootCausePoint(boolean rootCausePoint) { isRootCausePoint = rootCausePoint; }
}
