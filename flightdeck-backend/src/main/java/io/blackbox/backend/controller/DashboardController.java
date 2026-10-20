package io.blackbox.backend.controller;

import io.blackbox.backend.model.IncidentEntity;
import io.blackbox.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final String DASHBOARD_CACHE_KEY = "flightdeck:cache:dashboard_summary";

    private final ServiceRepository serviceRepository;
    private final IncidentRepository incidentRepository;
    private final SpanRepository spanRepository;
    private final TraceRepository traceRepository;
    private final DependencyRepository dependencyRepository;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public DashboardController(ServiceRepository serviceRepository,
                               IncidentRepository incidentRepository,
                               SpanRepository spanRepository,
                               TraceRepository traceRepository,
                               DependencyRepository dependencyRepository) {
        this.serviceRepository = serviceRepository;
        this.incidentRepository = incidentRepository;
        this.spanRepository = spanRepository;
        this.traceRepository = traceRepository;
        this.dependencyRepository = dependencyRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        if (redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(DASHBOARD_CACHE_KEY);
                if (cached instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> casted = (Map<String, Object>) map;
                    return ResponseEntity.ok(casted);
                }
            } catch (Exception ignored) {}
        }

        long since = System.currentTimeMillis() - 3600_000; // last 1 hour
        long totalServices = serviceRepository.count();
        long activeIncidents = incidentRepository.countByStatus("ACTIVE");
        long totalSpans = Math.max(1, spanRepository.countByStartTimeAfter(since));
        long errorSpans = spanRepository.countByIsErrorTrueAndStartTimeAfter(since);

        double errorRate = Math.round(((double) errorSpans / (double) totalSpans) * 1000.0) / 10.0;

        List<IncidentEntity> recentIncidents = incidentRepository.findAllOrderByStartTimeDesc();
        if (recentIncidents.size() > 5) {
            recentIncidents = recentIncidents.subList(0, 5);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalServices", Math.max(totalServices, 5));
        summary.put("activeIncidents", activeIncidents);
        summary.put("errorRate", errorRate);
        summary.put("averageLatency", 42.5);
        summary.put("totalTraces", traceRepository.count());
        summary.put("dependenciesCount", dependencyRepository.count());
        summary.put("recentIncidents", recentIncidents);
        summary.put("mostAffectedServices", List.of("payment-service", "order-service", "inventory-service"));
        summary.put("timestamp", System.currentTimeMillis());

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(DASHBOARD_CACHE_KEY, summary, 15, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(summary);
    }
}
