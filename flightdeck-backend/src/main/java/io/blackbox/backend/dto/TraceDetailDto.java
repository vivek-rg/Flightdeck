package io.blackbox.backend.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TraceDetailDto implements Serializable {
    private String traceId;
    private String rootServiceName;
    private String rootOperation;
    private Long startTime;
    private Long endTime;
    private Long durationMs;
    private int spanCount;
    private boolean hasError;
    private int errorCount;
    private List<String> servicesInvolved = new ArrayList<>();
    private TraceNodeDto rootNode;
    private List<TraceNodeDto> flatSpans = new ArrayList<>();

    public TraceDetailDto() {}

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

    public List<String> getServicesInvolved() { return servicesInvolved; }
    public void setServicesInvolved(List<String> servicesInvolved) { this.servicesInvolved = servicesInvolved; }

    public TraceNodeDto getRootNode() { return rootNode; }
    public void setRootNode(TraceNodeDto rootNode) { this.rootNode = rootNode; }

    public List<TraceNodeDto> getFlatSpans() { return flatSpans; }
    public void setFlatSpans(List<TraceNodeDto> flatSpans) { this.flatSpans = flatSpans; }
}
