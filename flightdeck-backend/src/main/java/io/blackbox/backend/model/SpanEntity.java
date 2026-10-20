package io.blackbox.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "spans", indexes = {
        @Index(name = "idx_spans_trace_id", columnList = "trace_id"),
        @Index(name = "idx_spans_service_name", columnList = "service_name"),
        @Index(name = "idx_spans_start_time", columnList = "start_time"),
        @Index(name = "idx_spans_status_code", columnList = "status_code"),
        @Index(name = "idx_spans_peer_service", columnList = "peer_service")
})
public class SpanEntity {

    @Id
    @Column(name = "span_id", length = 64)
    private String spanId;

    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    @Column(name = "parent_span_id", length = 64)
    private String parentSpanId;

    @Column(name = "service_name", nullable = false, length = 128)
    private String serviceName;

    @Column(name = "operation_name", nullable = false, length = 256)
    private String operationName;

    @Column(name = "span_kind", length = 32)
    private String spanKind; // SERVER, CLIENT, PRODUCER, CONSUMER, INTERNAL

    @Column(name = "start_time", nullable = false)
    private Long startTime;

    @Column(name = "end_time", nullable = false)
    private Long endTime;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "status_code", nullable = false, length = 32)
    private String statusCode; // OK, ERROR, UNSET

    @Column(name = "status_message", length = 1024)
    private String statusMessage;

    @Column(name = "is_error", nullable = false)
    private boolean isError;

    @Column(name = "http_method", length = 16)
    private String httpMethod;

    @Column(name = "http_route", length = 256)
    private String httpRoute;

    @Column(name = "peer_service", length = 128)
    private String peerService;

    @Column(name = "db_system", length = 64)
    private String dbSystem;

    @Column(name = "messaging_system", length = 64)
    private String messagingSystem;

    @Lob
    @Column(name = "attributes_json", columnDefinition = "TEXT")
    private String attributesJson;

    public SpanEntity() {}

    public String getSpanId() { return spanId; }
    public void setSpanId(String spanId) { this.spanId = spanId; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getParentSpanId() { return parentSpanId; }
    public void setParentSpanId(String parentSpanId) { this.parentSpanId = parentSpanId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getOperationName() { return operationName; }
    public void setOperationName(String operationName) { this.operationName = operationName; }

    public String getSpanKind() { return spanKind; }
    public void setSpanKind(String spanKind) { this.spanKind = spanKind; }

    public Long getStartTime() { return startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }

    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public boolean isError() { return isError; }
    public void setError(boolean error) { isError = error; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getHttpRoute() { return httpRoute; }
    public void setHttpRoute(String httpRoute) { this.httpRoute = httpRoute; }

    public String getPeerService() { return peerService; }
    public void setPeerService(String peerService) { this.peerService = peerService; }

    public String getDbSystem() { return dbSystem; }
    public void setDbSystem(String dbSystem) { this.dbSystem = dbSystem; }

    public String getMessagingSystem() { return messagingSystem; }
    public void setMessagingSystem(String messagingSystem) { this.messagingSystem = messagingSystem; }

    public String getAttributesJson() { return attributesJson; }
    public void setAttributesJson(String attributesJson) { this.attributesJson = attributesJson; }
}
