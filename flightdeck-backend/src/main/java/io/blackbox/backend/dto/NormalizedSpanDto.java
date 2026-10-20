package io.blackbox.backend.dto;

import java.io.Serializable;
import java.util.Map;

public class NormalizedSpanDto implements Serializable {
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String serviceName;
    private String operationName;
    private String spanKind; // SERVER, CLIENT, PRODUCER, CONSUMER, INTERNAL
    private Long startTime;
    private Long endTime;
    private Long durationMs;
    private String statusCode; // OK, ERROR
    private String statusMessage;
    private String httpMethod;
    private String httpRoute;
    private String peerService;
    private String dbSystem;
    private String messagingSystem;
    private Map<String, Object> attributes;

    public NormalizedSpanDto() {}

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getSpanId() { return spanId; }
    public void setSpanId(String spanId) { this.spanId = spanId; }

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

    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
}
