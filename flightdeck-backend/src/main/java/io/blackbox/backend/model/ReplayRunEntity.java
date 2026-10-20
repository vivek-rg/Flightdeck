package io.blackbox.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "replay_runs", indexes = {
        @Index(name = "idx_replay_incident_id", columnList = "incident_id"),
        @Index(name = "idx_replay_status", columnList = "status")
})
public class ReplayRunEntity {

    @Id
    @Column(name = "replay_id", length = 64)
    private String replayId;

    @Column(name = "incident_id", nullable = false, length = 64)
    private String incidentId;

    @Column(name = "start_time", nullable = false)
    private Long startTime;

    @Column(name = "end_time")
    private Long endTime;

    @Column(name = "status", nullable = false, length = 32)
    private String status; // PENDING, RUNNING, REPRODUCED, FAILED

    @Column(name = "reproduced_rate")
    private Double reproducedRate; // 0.0 to 1.0

    @Lob
    @Column(name = "failures_reproduced_json", columnDefinition = "TEXT")
    private String failuresReproducedJson;

    @Lob
    @Column(name = "events_reproduced_json", columnDefinition = "TEXT")
    private String eventsReproducedJson;

    @Lob
    @Column(name = "differences_json", columnDefinition = "TEXT")
    private String differencesJson;

    @Column(name = "comparison_summary", length = 1024)
    private String comparisonSummary;

    public ReplayRunEntity() {}

    public String getReplayId() { return replayId; }
    public void setReplayId(String replayId) { this.replayId = replayId; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public Long getStartTime() { return startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }

    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getReproducedRate() { return reproducedRate; }
    public void setReproducedRate(Double reproducedRate) { this.reproducedRate = reproducedRate; }

    public String getFailuresReproducedJson() { return failuresReproducedJson; }
    public void setFailuresReproducedJson(String failuresReproducedJson) { this.failuresReproducedJson = failuresReproducedJson; }

    public String getEventsReproducedJson() { return eventsReproducedJson; }
    public void setEventsReproducedJson(String eventsReproducedJson) { this.eventsReproducedJson = eventsReproducedJson; }

    public String getDifferencesJson() { return differencesJson; }
    public void setDifferencesJson(String differencesJson) { this.differencesJson = differencesJson; }

    public String getComparisonSummary() { return comparisonSummary; }
    public void setComparisonSummary(String comparisonSummary) { this.comparisonSummary = comparisonSummary; }
}
