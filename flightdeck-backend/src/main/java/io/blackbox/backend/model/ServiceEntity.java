package io.blackbox.backend.model;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "services", indexes = {
        @Index(name = "idx_services_name", columnList = "service_name", unique = true)
})
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false, unique = true, length = 128)
    private String serviceName;

    @Column(name = "service_type", length = 64)
    private String serviceType; // HTTP_SERVICE, DATABASE, CACHE, BROKER

    @Column(name = "first_seen")
    private Long firstSeen;

    @Column(name = "last_seen")
    private Long lastSeen;

    @Column(name = "status", length = 32)
    private String status; // HEALTHY, DEGRADED, FAILING

    public ServiceEntity() {}

    public ServiceEntity(String serviceName, String serviceType) {
        this.serviceName = serviceName;
        this.serviceType = serviceType;
        this.firstSeen = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
        this.status = "HEALTHY";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public Long getFirstSeen() { return firstSeen; }
    public void setFirstSeen(Long firstSeen) { this.firstSeen = firstSeen; }

    public Long getLastSeen() { return lastSeen; }
    public void setLastSeen(Long lastSeen) { this.lastSeen = lastSeen; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceEntity that = (ServiceEntity) o;
        return Objects.equals(serviceName, that.serviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName);
    }
}
