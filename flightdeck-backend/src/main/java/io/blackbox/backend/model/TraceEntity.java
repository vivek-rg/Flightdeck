package io.blackbox.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "traces", indexes = {
        @Index(name = "idx_traces_start_time", columnList = "start_time"),
        @Index(name = "idx_traces_has_error", columnList = "has_error"),
        @Index(name = "idx_traces_root_service", columnList = "root_service_name")
})
public class TraceEntity {

    @Id
    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "root_service_name", length = 128)
    private String rootServiceName;

    @Column(name = "root_operation", length = 256)
    private String rootOperation;

    @Column(name = "start_time", nullable = false)
    private Long startTime;

    @Column(name = "end_time", nullable = false)
    private Long endTime;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "span_count", nullable = false)
    private int spanCount;

    @Column(name = "has_error", nullable = false)
    private boolean hasError;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "services_involved", length = 512)
    private String servicesInvolved;

    public TraceEntity() {}

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getRootServiceName() { return rootServiceName; }
    public void setRootServiceName(String rootServiceName) { this.rootServiceName = rootServiceName; }

    public String getRootOperation() { return rootOperation; }
    public void setRootOperation(String rootOperation) { this.rootOperation = rootOperation; }

    public Long getStartTime() { return startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }

    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public int getSpanCount() { return spanCount; }
    public void setSpanCount(int spanCount) { this.spanCount = spanCount; }

    public boolean isHasError() { return hasError; }
    public void setHasError(boolean hasError) { this.hasError = hasError; }

    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

    public String getServicesInvolved() { return servicesInvolved; }
    public void setServicesInvolved(String servicesInvolved) { this.servicesInvolved = servicesInvolved; }
}
