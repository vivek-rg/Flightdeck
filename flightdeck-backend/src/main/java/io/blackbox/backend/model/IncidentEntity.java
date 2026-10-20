package io.blackbox.backend.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidents", indexes = {
        @Index(name = "idx_incidents_start_time", columnList = "start_time"),
        @Index(name = "idx_incidents_status", columnList = "status"),
        @Index(name = "idx_incidents_severity", columnList = "severity")
})
public class IncidentEntity {

    @Id
    @Column(name = "incident_id", length = 64)
    private String incidentId;

    @Column(name = "start_time", nullable = false)
    private Long startTime;

    @Column(name = "end_time")
    private Long endTime;

    @Column(name = "severity", nullable = false, length = 32)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "status", nullable = false, length = 32)
    private String status; // ACTIVE, RESOLVED, REPLAYED

    @Column(name = "trigger_service", length = 128)
    private String triggerService;

    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "root_cause_candidate", length = 256)
    private String rootCauseCandidate;

    @Column(name = "root_cause_confidence")
    private Double rootCauseConfidence;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("timestamp ASC")
    private List<IncidentEventEntity> events = new ArrayList<>();

    public IncidentEntity() {}

    public IncidentEntity(String incidentId, Long startTime, String severity, String status, String triggerService, String description) {
        this.incidentId = incidentId;
        this.startTime = startTime;
        this.severity = severity;
        this.status = status;
        this.triggerService = triggerService;
        this.description = description;
    }

    public void addEvent(IncidentEventEntity event) {
        events.add(event);
        event.setIncident(this);
    }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public Long getStartTime() { return startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }

    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTriggerService() { return triggerService; }
    public void setTriggerService(String triggerService) { this.triggerService = triggerService; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRootCauseCandidate() { return rootCauseCandidate; }
    public void setRootCauseCandidate(String rootCauseCandidate) { this.rootCauseCandidate = rootCauseCandidate; }

    public Double getRootCauseConfidence() { return rootCauseConfidence; }
    public void setRootCauseConfidence(Double rootCauseConfidence) { this.rootCauseConfidence = rootCauseConfidence; }

    public List<IncidentEventEntity> getEvents() { return events; }
    public void setEvents(List<IncidentEventEntity> events) { this.events = events; }
}
