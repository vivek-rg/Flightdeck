package io.blackbox.backend.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.blackbox.backend.dto.NormalizedSpanDto;
import io.blackbox.backend.model.AnomalyEntity;
import io.blackbox.backend.model.SpanEntity;
import io.blackbox.backend.model.TraceEntity;
import io.blackbox.backend.repository.SpanRepository;
import io.blackbox.backend.repository.TraceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TelemetryProcessorService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryProcessorService.class);

    private final SpanRepository spanRepository;
    private final TraceRepository traceRepository;
    private final DependencyDiscoveryService dependencyDiscoveryService;
    private final AnomalyDetectionEngine anomalyDetectionEngine;
    private final IncidentDetectionEngine incidentDetectionEngine;
    private final ObjectMapper objectMapper;

    public TelemetryProcessorService(SpanRepository spanRepository,
                                     TraceRepository traceRepository,
                                     DependencyDiscoveryService dependencyDiscoveryService,
                                     AnomalyDetectionEngine anomalyDetectionEngine,
                                     IncidentDetectionEngine incidentDetectionEngine,
                                     ObjectMapper objectMapper) {
        this.spanRepository = spanRepository;
        this.traceRepository = traceRepository;
        this.dependencyDiscoveryService = dependencyDiscoveryService;
        this.anomalyDetectionEngine = anomalyDetectionEngine;
        this.incidentDetectionEngine = incidentDetectionEngine;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SpanEntity processNormalizedSpan(NormalizedSpanDto dto) {
        SpanEntity entity = toEntity(dto);
        spanRepository.save(entity);

        // 1. Update or create Trace summary
        updateTraceAggregate(entity);

        // 2. Discover service dependencies
        dependencyDiscoveryService.processSpan(entity);

        // 3. Run anomaly detection
        List<AnomalyEntity> anomalies = anomalyDetectionEngine.analyzeSpan(entity);

        // 4. If failure, error, or anomaly detected, feed to Incident Detection Engine
        if (entity.isError() || !anomalies.isEmpty()) {
            incidentDetectionEngine.processFailureOrAnomaly(entity, anomalies);
        }

        return entity;
    }

    @Transactional
    public List<SpanEntity> processBatch(List<NormalizedSpanDto> dtos) {
        List<SpanEntity> entities = new ArrayList<>();
        for (NormalizedSpanDto dto : dtos) {
            entities.add(processNormalizedSpan(dto));
        }
        return entities;
    }

    private void updateTraceAggregate(SpanEntity span) {
        String traceId = span.getTraceId();
        Optional<TraceEntity> opt = traceRepository.findById(traceId);

        TraceEntity trace;
        if (opt.isPresent()) {
            trace = opt.get();
            trace.setStartTime(Math.min(trace.getStartTime(), span.getStartTime()));
            trace.setEndTime(Math.max(trace.getEndTime(), span.getEndTime()));
            trace.setDurationMs(Math.max(1, trace.getEndTime() - trace.getStartTime()));
            trace.setSpanCount(trace.getSpanCount() + 1);
            if (span.isError()) {
                trace.setHasError(true);
                trace.setErrorCount(trace.getErrorCount() + 1);
            }
            if (span.getParentSpanId() == null || span.getParentSpanId().isBlank()) {
                trace.setRootServiceName(span.getServiceName());
                trace.setRootOperation(span.getOperationName());
            }

            // Append service if not present
            if (trace.getServicesInvolved() == null || !trace.getServicesInvolved().contains(span.getServiceName())) {
                String current = trace.getServicesInvolved() != null ? trace.getServicesInvolved() + "," : "";
                trace.setServicesInvolved(current + span.getServiceName());
            }
        } else {
            trace = new TraceEntity();
            trace.setTraceId(traceId);
            trace.setRootServiceName(span.getServiceName());
            trace.setRootOperation(span.getOperationName());
            trace.setStartTime(span.getStartTime());
            trace.setEndTime(span.getEndTime());
            trace.setDurationMs(Math.max(1, span.getDurationMs()));
            trace.setSpanCount(1);
            trace.setHasError(span.isError());
            trace.setErrorCount(span.isError() ? 1 : 0);
            trace.setServicesInvolved(span.getServiceName());
        }

        traceRepository.save(trace);
    }

    private SpanEntity toEntity(NormalizedSpanDto dto) {
        SpanEntity s = new SpanEntity();
        s.setSpanId(dto.getSpanId() != null ? dto.getSpanId() : UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        s.setTraceId(dto.getTraceId() != null ? dto.getTraceId() : UUID.randomUUID().toString().replace("-", ""));
        s.setParentSpanId(dto.getParentSpanId());
        s.setServiceName(dto.getServiceName() != null ? dto.getServiceName() : "unknown-service");
        s.setOperationName(dto.getOperationName() != null ? dto.getOperationName() : "operation");
        s.setSpanKind(dto.getSpanKind() != null ? dto.getSpanKind() : "INTERNAL");

        long start = dto.getStartTime() != null ? dto.getStartTime() : System.currentTimeMillis();
        long end = dto.getEndTime() != null ? dto.getEndTime() : start + (dto.getDurationMs() != null ? dto.getDurationMs() : 10);
        long dur = dto.getDurationMs() != null ? dto.getDurationMs() : Math.max(1, end - start);

        s.setStartTime(start);
        s.setEndTime(end);
        s.setDurationMs(dur);

        boolean error = "ERROR".equalsIgnoreCase(dto.getStatusCode()) || (dto.getStatusMessage() != null && dto.getStatusMessage().toLowerCase().contains("error"));
        s.setStatusCode(error ? "ERROR" : "OK");
        s.setStatusMessage(dto.getStatusMessage());
        s.setError(error);

        s.setHttpMethod(dto.getHttpMethod());
        s.setHttpRoute(dto.getHttpRoute());
        s.setPeerService(dto.getPeerService());
        s.setDbSystem(dto.getDbSystem());
        s.setMessagingSystem(dto.getMessagingSystem());

        if (dto.getAttributes() != null) {
            try {
                s.setAttributesJson(objectMapper.writeValueAsString(dto.getAttributes()));
            } catch (Exception ignored) {}
        }
        return s;
    }
}
