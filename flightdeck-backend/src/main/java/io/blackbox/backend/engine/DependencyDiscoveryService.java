package io.blackbox.backend.engine;

import io.blackbox.backend.model.DependencyEntity;
import io.blackbox.backend.model.ServiceEntity;
import io.blackbox.backend.model.SpanEntity;
import io.blackbox.backend.repository.DependencyRepository;
import io.blackbox.backend.repository.ServiceRepository;
import io.blackbox.backend.repository.SpanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class DependencyDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(DependencyDiscoveryService.class);
    private static final String REDIS_GRAPH_CACHE_KEY = "flightdeck:cache:dependency_graph";

    private final DependencyRepository dependencyRepository;
    private final ServiceRepository serviceRepository;
    private final SpanRepository spanRepository;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public DependencyDiscoveryService(DependencyRepository dependencyRepository,
                                      ServiceRepository serviceRepository,
                                      SpanRepository spanRepository) {
        this.dependencyRepository = dependencyRepository;
        this.serviceRepository = serviceRepository;
        this.spanRepository = spanRepository;
    }

    @Transactional
    public void processSpan(SpanEntity span) {
        // Register service if not already registered
        registerService(span.getServiceName(), "MICROSERVICE");

        String source = span.getServiceName();
        String destination = null;
        String type = "HTTP";

        // 1. Database dependencies (Redis, Postgres)
        if (span.getDbSystem() != null && !span.getDbSystem().isBlank()) {
            destination = span.getDbSystem().toLowerCase();
            type = destination.toUpperCase();
            registerService(destination, "DATABASE");
        }
        // 2. Messaging producer dependencies (Kafka)
        else if ("PRODUCER".equalsIgnoreCase(span.getSpanKind()) || "kafka".equalsIgnoreCase(span.getMessagingSystem())) {
            destination = "kafka";
            type = "KAFKA";
            registerService("kafka", "BROKER");
        }
        // 3. Messaging consumer dependencies (Kafka -> Service)
        else if ("CONSUMER".equalsIgnoreCase(span.getSpanKind())) {
            source = "kafka";
            destination = span.getServiceName();
            type = "KAFKA";
            registerService("kafka", "BROKER");
        }
        // 4. HTTP client calls to peer services
        else if ("CLIENT".equalsIgnoreCase(span.getSpanKind()) && span.getPeerService() != null) {
            destination = normalizePeerService(span.getPeerService());
            type = "HTTP";
            registerService(destination, "MICROSERVICE");
        }
        // 5. Parent-Child Service Linkage (Cross-service call inference)
        else if (span.getParentSpanId() != null && !span.getParentSpanId().isBlank()) {
            Optional<SpanEntity> parentOpt = spanRepository.findById(span.getParentSpanId());
            if (parentOpt.isPresent()) {
                SpanEntity parent = parentOpt.get();
                if (!parent.getServiceName().equalsIgnoreCase(span.getServiceName())) {
                    source = parent.getServiceName();
                    destination = span.getServiceName();
                    type = "HTTP";
                }
            }
        }

        if (destination != null && !destination.equalsIgnoreCase(source)) {
            recordDependency(source, destination, type, span.getDurationMs(), span.isError());
        }
    }

    @Transactional
    public void recordDependency(String source, String destination, String type, long durationMs, boolean isError) {
        Optional<DependencyEntity> opt = dependencyRepository
                .findBySourceServiceAndDestinationServiceAndDependencyType(source, destination, type);

        DependencyEntity entity;
        if (opt.isPresent()) {
            entity = opt.get();
        } else {
            entity = new DependencyEntity(source, destination, type);
            log.info("DISCOVERED NEW DEPENDENCY: {} -> {} ({})", source, destination, type);
        }

        entity.recordCall(durationMs, isError);
        dependencyRepository.save(entity);

        invalidateCache();
    }

    @Transactional
    public void registerService(String serviceName, String defaultType) {
        if (serviceName == null || serviceName.isBlank()) return;
        String cleanName = serviceName.trim().toLowerCase();
        Optional<ServiceEntity> opt = serviceRepository.findByServiceName(cleanName);
        if (opt.isEmpty()) {
            ServiceEntity entity = new ServiceEntity(cleanName, defaultType);
            serviceRepository.save(entity);
            log.info("DISCOVERED NEW SERVICE: {} (type: {})", cleanName, defaultType);
        } else {
            ServiceEntity entity = opt.get();
            entity.setLastSeen(System.currentTimeMillis());
            serviceRepository.save(entity);
        }
    }

    public Map<String, Object> getGraphData() {
        // Check Redis cache first
        if (redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(REDIS_GRAPH_CACHE_KEY);
                if (cached instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) cached;
                    return map;
                }
            } catch (Exception ex) {
                log.warn("Redis graph cache read failed: {}", ex.getMessage());
            }
        }

        List<ServiceEntity> services = serviceRepository.findAll();
        List<DependencyEntity> dependencies = dependencyRepository.findAll();

        List<Map<String, Object>> nodes = new ArrayList<>();
        for (ServiceEntity s : services) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", s.getServiceName());
            node.put("name", s.getServiceName());
            node.put("type", s.getServiceType());
            node.put("status", s.getStatus());
            node.put("lastSeen", s.getLastSeen());
            nodes.add(node);
        }

        List<Map<String, Object>> edges = new ArrayList<>();
        for (DependencyEntity d : dependencies) {
            Map<String, Object> edge = new HashMap<>();
            edge.put("id", d.getSourceService() + "->" + d.getDestinationService() + ":" + d.getDependencyType());
            edge.put("source", d.getSourceService());
            edge.put("target", d.getDestinationService());
            edge.put("type", d.getDependencyType());
            edge.put("requestCount", d.getRequestCount());
            edge.put("errorCount", d.getErrorCount());
            edge.put("averageLatency", Math.round(d.getAverageLatency() * 100.0) / 100.0);
            edges.add(edge);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);
        result.put("timestamp", System.currentTimeMillis());

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(REDIS_GRAPH_CACHE_KEY, result, 60, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        }

        return result;
    }

    public List<DependencyEntity> getDependenciesForService(String serviceName) {
        return dependencyRepository.findBySourceService(serviceName);
    }

    public List<DependencyEntity> getDependentsOnService(String serviceName) {
        return dependencyRepository.findByDestinationService(serviceName);
    }

    private void invalidateCache() {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(REDIS_GRAPH_CACHE_KEY);
            } catch (Exception ignored) {}
        }
    }

    private String normalizePeerService(String peer) {
        if (peer == null) return "unknown";
        String p = peer.toLowerCase();
        if (p.contains("order")) return "order-service";
        if (p.contains("payment")) return "payment-service";
        if (p.contains("inventory")) return "inventory-service";
        if (p.contains("notification")) return "notification-service";
        if (p.contains("gateway")) return "api-gateway";
        if (p.contains("redis")) return "redis";
        if (p.contains("postgres")) return "postgresql";
        if (p.contains("kafka")) return "kafka";
        return p;
    }
}
