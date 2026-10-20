package io.blackbox.backend.config;

import io.blackbox.backend.engine.DependencyDiscoveryService;
import io.blackbox.backend.model.DependencyEntity;
import io.blackbox.backend.model.IncidentEntity;
import io.blackbox.backend.model.IncidentEventEntity;
import io.blackbox.backend.model.ServiceEntity;
import io.blackbox.backend.repository.DependencyRepository;
import io.blackbox.backend.repository.IncidentEventRepository;
import io.blackbox.backend.repository.IncidentRepository;
import io.blackbox.backend.repository.ServiceRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ServiceRepository serviceRepository;
    private final DependencyRepository dependencyRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository eventRepository;

    public DataInitializer(ServiceRepository serviceRepository,
                           DependencyRepository dependencyRepository,
                           IncidentRepository incidentRepository,
                           IncidentEventRepository eventRepository) {
        this.serviceRepository = serviceRepository;
        this.dependencyRepository = dependencyRepository;
        this.incidentRepository = incidentRepository;
        this.eventRepository = eventRepository;
    }

    @PostConstruct
    public void initBaseline() {
        if (serviceRepository.count() == 0) {
            log.info("Initializing baseline service catalogue and initial observed dependencies...");
            serviceRepository.save(new ServiceEntity("api-gateway", "MICROSERVICE"));
            serviceRepository.save(new ServiceEntity("order-service", "MICROSERVICE"));
            serviceRepository.save(new ServiceEntity("payment-service", "MICROSERVICE"));
            serviceRepository.save(new ServiceEntity("inventory-service", "MICROSERVICE"));
            serviceRepository.save(new ServiceEntity("notification-service", "MICROSERVICE"));
            serviceRepository.save(new ServiceEntity("redis", "CACHE"));
            serviceRepository.save(new ServiceEntity("postgresql", "DATABASE"));
            serviceRepository.save(new ServiceEntity("kafka", "BROKER"));

            // Initialize sample discovered dependencies
            DependencyEntity d1 = new DependencyEntity("api-gateway", "order-service", "HTTP");
            d1.recordCall(45, false);
            d1.recordCall(38, false);

            DependencyEntity d2 = new DependencyEntity("order-service", "payment-service", "HTTP");
            d2.recordCall(85, false);

            DependencyEntity d3 = new DependencyEntity("payment-service", "redis", "REDIS");
            d3.recordCall(8, false);

            DependencyEntity d4 = new DependencyEntity("order-service", "inventory-service", "HTTP");
            d4.recordCall(60, false);

            DependencyEntity d5 = new DependencyEntity("inventory-service", "postgresql", "POSTGRESQL");
            d5.recordCall(12, false);

            DependencyEntity d6 = new DependencyEntity("order-service", "kafka", "KAFKA");
            d6.recordCall(15, false);

            DependencyEntity d7 = new DependencyEntity("kafka", "notification-service", "KAFKA");
            d7.recordCall(22, false);

            dependencyRepository.saveAll(List.of(d1, d2, d3, d4, d5, d6, d7));
            log.info("Initialized baseline dependency graph with 7 topology edges.");
        }
    }
}
