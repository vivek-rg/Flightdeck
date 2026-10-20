package io.blackbox.backend.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TraceNodeDto implements Serializable {
    private String spanId;
    private String parentSpanId;
    private String serviceName;
    private String operationName;
    private String spanKind;
    private Long startTime;
    private Long endTime;
    private Long durationMs;
    private String statusCode;
    private String statusMessage;
    private boolean isError;
    private String peerService;
    private String dbSystem;
    private String messagingSystem;
    private String attributesJson;
    private List<TraceNodeDto> children = new ArrayList<>();

    public TraceNodeDto() {}

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

    public boolean isError() { return isError; }
    public void setError(boolean error) { isError = error; }

    public String getPeerService() { return peerService; }
    public void setPeerService(String peerService) { this.peerService = peerService; }

    public String getDbSystem() { return dbSystem; }
    public void setDbSystem(String dbSystem) { this.dbSystem = dbSystem; }

    public String getMessagingSystem() { return messagingSystem; }
    public void setMessagingSystem(String messagingSystem) { this.messagingSystem = messagingSystem; }

    public String getAttributesJson() { return attributesJson; }
    public void setAttributesJson(String attributesJson) { this.attributesJson = attributesJson; }

    public List<TraceNodeDto> getChildren() { return children; }
    public void setChildren(List<TraceNodeDto> children) { this.children = children; }
}
