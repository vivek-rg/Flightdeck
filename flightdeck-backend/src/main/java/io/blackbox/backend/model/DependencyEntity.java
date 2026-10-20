package io.blackbox.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "dependencies", indexes = {
        @Index(name = "idx_deps_src_dst", columnList = "source_service, destination_service, dependency_type", unique = true),
        @Index(name = "idx_deps_source", columnList = "source_service"),
        @Index(name = "idx_deps_destination", columnList = "destination_service")
})
public class DependencyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_service", nullable = false, length = 128)
    private String sourceService;

    @Column(name = "destination_service", nullable = false, length = 128)
    private String destinationService;

    @Column(name = "dependency_type", nullable = false, length = 64)
    private String dependencyType; // HTTP, REDIS, POSTGRESQL, KAFKA

    @Column(name = "first_seen", nullable = false)
    private Long firstSeen;

    @Column(name = "last_seen", nullable = false)
    private Long lastSeen;

    @Column(name = "request_count", nullable = false)
    private long requestCount;

    @Column(name = "error_count", nullable = false)
    private long errorCount;

    @Column(name = "average_latency", nullable = false)
    private double averageLatency;

    @Column(name = "p95_latency")
    private double p95Latency;

    @Column(name = "p99_latency")
    private double p99Latency;

    public DependencyEntity() {}

    public DependencyEntity(String sourceService, String destinationService, String dependencyType) {
        this.sourceService = sourceService;
        this.destinationService = destinationService;
        this.dependencyType = dependencyType;
        this.firstSeen = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
        this.requestCount = 0;
        this.errorCount = 0;
        this.averageLatency = 0.0;
    }

    public void recordCall(long durationMs, boolean isError) {
        this.lastSeen = System.currentTimeMillis();
        this.requestCount++;
        if (isError) {
            this.errorCount++;
        }
        // Exponential moving average or cumulative average
        if (this.requestCount == 1) {
            this.averageLatency = durationMs;
        } else {
            this.averageLatency = (this.averageLatency * (this.requestCount - 1) + durationMs) / this.requestCount;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSourceService() { return sourceService; }
    public void setSourceService(String sourceService) { this.sourceService = sourceService; }

    public String getDestinationService() { return destinationService; }
    public void setDestinationService(String destinationService) { this.destinationService = destinationService; }

    public String getDependencyType() { return dependencyType; }
    public void setDependencyType(String dependencyType) { this.dependencyType = dependencyType; }

    public Long getFirstSeen() { return firstSeen; }
    public void setFirstSeen(Long firstSeen) { this.firstSeen = firstSeen; }

    public Long getLastSeen() { return lastSeen; }
    public void setLastSeen(Long lastSeen) { this.lastSeen = lastSeen; }

    public long getRequestCount() { return requestCount; }
    public void setRequestCount(long requestCount) { this.requestCount = requestCount; }

    public long getErrorCount() { return errorCount; }
    public void setErrorCount(long errorCount) { this.errorCount = errorCount; }

    public double getAverageLatency() { return averageLatency; }
    public void setAverageLatency(double averageLatency) { this.averageLatency = averageLatency; }

    public double getP95Latency() { return p95Latency; }
    public void setP95Latency(double p95Latency) { this.p95Latency = p95Latency; }

    public double getP99Latency() { return p99Latency; }
    public void setP99Latency(double p99Latency) { this.p99Latency = p99Latency; }
}
