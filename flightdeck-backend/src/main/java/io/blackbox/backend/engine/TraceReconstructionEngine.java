package io.blackbox.backend.engine;

import io.blackbox.backend.dto.TraceDetailDto;
import io.blackbox.backend.dto.TraceNodeDto;
import io.blackbox.backend.model.SpanEntity;
import io.blackbox.backend.model.TraceEntity;
import io.blackbox.backend.repository.SpanRepository;
import io.blackbox.backend.repository.TraceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TraceReconstructionEngine {

    private static final Logger log = LoggerFactory.getLogger(TraceReconstructionEngine.class);
    private static final String TRACE_CACHE_PREFIX = "flightdeck:cache:trace:";

    private final SpanRepository spanRepository;
    private final TraceRepository traceRepository;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public TraceReconstructionEngine(SpanRepository spanRepository, TraceRepository traceRepository) {
        this.spanRepository = spanRepository;
        this.traceRepository = traceRepository;
    }

    public TraceDetailDto reconstructTrace(String traceId) {
        // Redis Cache check
        if (redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(TRACE_CACHE_PREFIX + traceId);
                if (cached instanceof TraceDetailDto detail) {
                    return detail;
                }
            } catch (Exception ex) {
                log.warn("Redis trace cache read failed: {}", ex.getMessage());
            }
        }

        List<SpanEntity> spans = spanRepository.findByTraceIdOrderByStartTimeAsc(traceId);
        if (spans.isEmpty()) {
            throw new NoSuchElementException("Trace not found: " + traceId);
        }

        TraceDetailDto detail = new TraceDetailDto();
        detail.setTraceId(traceId);

        long minStart = Long.MAX_VALUE;
        long maxEnd = Long.MIN_VALUE;
        int errorCount = 0;
        Set<String> services = new LinkedHashSet<>();

        Map<String, TraceNodeDto> nodeMap = new HashMap<>();
        List<TraceNodeDto> flatNodes = new ArrayList<>();

        for (SpanEntity span : spans) {
            TraceNodeDto node = toNodeDto(span);
            nodeMap.put(span.getSpanId(), node);
            flatNodes.add(node);

            minStart = Math.min(minStart, span.getStartTime());
            maxEnd = Math.max(maxEnd, span.getEndTime());
            if (span.isError()) {
                errorCount++;
            }
            services.add(span.getServiceName());
            if (span.getPeerService() != null && !span.getPeerService().isBlank()) {
                services.add(span.getPeerService());
            }
        }

        detail.setStartTime(minStart);
        detail.setEndTime(maxEnd);
        detail.setDurationMs(Math.max(1, maxEnd - minStart));
        detail.setSpanCount(spans.size());
        detail.setHasError(errorCount > 0);
        detail.setErrorCount(errorCount);
        detail.setServicesInvolved(new ArrayList<>(services));
        detail.setFlatSpans(flatNodes);

        // Build tree: link children to parent
        TraceNodeDto rootNode = null;
        for (SpanEntity span : spans) {
            TraceNodeDto node = nodeMap.get(span.getSpanId());
            String parentId = span.getParentSpanId();

            if (parentId != null && nodeMap.containsKey(parentId)) {
                nodeMap.get(parentId).getChildren().add(node);
            } else {
                // If multiple roots, pick the earliest or create virtual root
                if (rootNode == null) {
                    rootNode = node;
                } else {
                    // Sibling root attached to earlier root
                    rootNode.getChildren().add(node);
                }
            }
        }

        detail.setRootNode(rootNode != null ? rootNode : (flatNodes.isEmpty() ? null : flatNodes.get(0)));
        if (rootNode != null) {
            detail.setRootServiceName(rootNode.getServiceName());
            detail.setRootOperation(rootNode.getOperationName());
        }

        // Cache in Redis
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(TRACE_CACHE_PREFIX + traceId, detail, 120, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        }

        return detail;
    }

    private TraceNodeDto toNodeDto(SpanEntity entity) {
        TraceNodeDto dto = new TraceNodeDto();
        dto.setSpanId(entity.getSpanId());
        dto.setParentSpanId(entity.getParentSpanId());
        dto.setServiceName(entity.getServiceName());
        dto.setOperationName(entity.getOperationName());
        dto.setSpanKind(entity.getSpanKind());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setDurationMs(entity.getDurationMs());
        dto.setStatusCode(entity.getStatusCode());
        dto.setStatusMessage(entity.getStatusMessage());
        dto.setError(entity.isError());
        dto.setPeerService(entity.getPeerService());
        dto.setDbSystem(entity.getDbSystem());
        dto.setMessagingSystem(entity.getMessagingSystem());
        dto.setAttributesJson(entity.getAttributesJson());
        return dto;
    }
}
