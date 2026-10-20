package io.blackbox.backend.engine;

import io.blackbox.backend.model.DependencyEntity;
import io.blackbox.backend.model.ServiceEntity;
import io.blackbox.backend.model.SpanEntity;
import io.blackbox.backend.repository.DependencyRepository;
import io.blackbox.backend.repository.ServiceRepository;
import io.blackbox.backend.repository.SpanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DependencyDiscoveryTest {

    @Mock
    private DependencyRepository dependencyRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private SpanRepository spanRepository;

    private DependencyDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        discoveryService = new DependencyDiscoveryService(dependencyRepository, serviceRepository, spanRepository);
    }

    @Test
    @DisplayName("Should automatically discover Redis dependency from span without hardcoding")
    void testDiscoverRedisDependency() {
        SpanEntity span = new SpanEntity();
        span.setSpanId("sp-101");
        span.setTraceId("tr-101");
        span.setServiceName("payment-service");
        span.setOperationName("REDIS GET payment_lock:123");
        span.setSpanKind("CLIENT");
        span.setDbSystem("redis");
        span.setDurationMs(480L);
        span.setError(false);

        when(serviceRepository.findByServiceName("payment-service")).thenReturn(Optional.of(new ServiceEntity("payment-service", "MICROSERVICE")));
        when(serviceRepository.findByServiceName("redis")).thenReturn(Optional.empty());
        when(dependencyRepository.findBySourceServiceAndDestinationServiceAndDependencyType("payment-service", "redis", "REDIS"))
                .thenReturn(Optional.empty());

        discoveryService.processSpan(span);

        ArgumentCaptor<DependencyEntity> depCaptor = ArgumentCaptor.forClass(DependencyEntity.class);
        verify(dependencyRepository).save(depCaptor.capture());

        DependencyEntity saved = depCaptor.getValue();
        assertEquals("payment-service", saved.getSourceService());
        assertEquals("redis", saved.getDestinationService());
        assertEquals("REDIS", saved.getDependencyType());
        assertEquals(1, saved.getRequestCount());
        assertEquals(480.0, saved.getAverageLatency());
    }

    @Test
    @DisplayName("Should automatically discover HTTP peer dependency between order-service and payment-service")
    void testDiscoverHttpDependency() {
        SpanEntity span = new SpanEntity();
        span.setSpanId("sp-102");
        span.setTraceId("tr-102");
        span.setServiceName("order-service");
        span.setOperationName("HTTP POST http://payment-service:8083/payments");
        span.setSpanKind("CLIENT");
        span.setPeerService("payment-service");
        span.setDurationMs(95L);
        span.setError(false);

        when(serviceRepository.findByServiceName(anyString())).thenReturn(Optional.empty());
        when(dependencyRepository.findBySourceServiceAndDestinationServiceAndDependencyType("order-service", "payment-service", "HTTP"))
                .thenReturn(Optional.empty());

        discoveryService.processSpan(span);

        ArgumentCaptor<DependencyEntity> depCaptor = ArgumentCaptor.forClass(DependencyEntity.class);
        verify(dependencyRepository).save(depCaptor.capture());

        DependencyEntity saved = depCaptor.getValue();
        assertEquals("order-service", saved.getSourceService());
        assertEquals("payment-service", saved.getDestinationService());
        assertEquals("HTTP", saved.getDependencyType());
    }
}
