package io.blackbox.sample.inventory.service;

import io.blackbox.sample.common.dto.InventoryReservationDto;
import io.blackbox.sample.common.dto.OrderItemDto;
import io.blackbox.sample.common.failure.FailureInjectionRegistry;
import io.blackbox.sample.common.tracing.TraceContextUtils;
import io.blackbox.sample.common.tracing.TracingHelper;
import io.blackbox.sample.inventory.model.InventoryEntity;
import io.blackbox.sample.inventory.repository.InventoryRepository;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final TracingHelper tracingHelper;
    private final FailureInjectionRegistry failureRegistry;

    public InventoryService(InventoryRepository inventoryRepository,
                            TracingHelper tracingHelper,
                            FailureInjectionRegistry failureRegistry) {
        this.inventoryRepository = inventoryRepository;
        this.tracingHelper = tracingHelper;
        this.failureRegistry = failureRegistry;
    }

    @PostConstruct
    public void initStock() {
        if (inventoryRepository.count() == 0) {
            inventoryRepository.save(new InventoryEntity("PROD-1", "Quantum Wireless Headphones", 500, 0));
            inventoryRepository.save(new InventoryEntity("PROD-2", "OLED Smart Display", 250, 0));
            inventoryRepository.save(new InventoryEntity("PROD-3", "Mechanical Gaming Keyboard", 100, 0));
            log.info("Initialized default stock for PROD-1, PROD-2, PROD-3 in PostgreSQL");
        }
    }

    @Transactional
    public InventoryReservationDto reserveStock(InventoryReservationDto request) {
        String traceId = TraceContextUtils.getCurrentTraceId();
        log.info("Reserving stock for order: orderId={}, traceId={}", request.getOrderId(), traceId);

        if (failureRegistry.isServiceUnavailable()) {
            log.error("FAILURE INJECTION: Inventory service simulated 503 Unavailable");
            Span.current().setStatus(StatusCode.ERROR, "Service 503 Unavailable");
            throw new RuntimeException("Service Unavailable: Inventory service capacity reached");
        }

        try {
            return tracingHelper.traceDatabaseOperation("SELECT_FOR_UPDATE", "inventory",
                    "SELECT * FROM inventory WHERE product_id IN (?) FOR UPDATE", () -> {

                if (failureRegistry.isDatabaseUnavailable()) {
                    log.error("FAILURE INJECTION: Database connection failure simulated on postgresql:5432");
                    throw new RuntimeException("PSQLException: Connection refused to postgresql://postgres:5432/blackbox_sample");
                }

                if (request.getItems() != null) {
                    for (OrderItemDto item : request.getItems()) {
                        Optional<InventoryEntity> opt = inventoryRepository.findById(item.getProductId());
                        InventoryEntity entity;
                        if (opt.isPresent()) {
                            entity = opt.get();
                        } else {
                            entity = new InventoryEntity(item.getProductId(), "Product " + item.getProductId(), 100, 0);
                        }

                        if (entity.getAvailableQuantity() < item.getQuantity()) {
                            log.warn("Insufficient stock for product {}: requested={}, available={}",
                                    item.getProductId(), item.getQuantity(), entity.getAvailableQuantity());
                            InventoryReservationDto failed = new InventoryReservationDto(
                                    request.getOrderId(), request.getItems(), "FAILED",
                                    "Insufficient stock for " + item.getProductId());
                            failed.setTraceId(traceId);
                            return failed;
                        }

                        entity.setAvailableQuantity(entity.getAvailableQuantity() - item.getQuantity());
                        entity.setReservedQuantity(entity.getReservedQuantity() + item.getQuantity());
                        entity.setUpdatedAt(System.currentTimeMillis());
                        inventoryRepository.save(entity);
                    }
                }

                InventoryReservationDto success = new InventoryReservationDto(
                        request.getOrderId(), request.getItems(), "RESERVED", "Stock reserved successfully");
                success.setTraceId(traceId);
                return success;
            });
        } catch (Exception ex) {
            log.error("Database operation failed in InventoryService: {}", ex.getMessage());
            Span.current().setStatus(StatusCode.ERROR, ex.getMessage());
            Span.current().setAttribute(AttributeKey.stringKey("error.type"), ex.getClass().getName());
            InventoryReservationDto errorRes = new InventoryReservationDto(
                    request.getOrderId(), request.getItems(), "FAILED", "Database error: " + ex.getMessage());
            errorRes.setTraceId(traceId);
            return errorRes;
        }
    }

    @Transactional(readOnly = true)
    public InventoryEntity getStock(String productId) {
        try {
            return tracingHelper.traceDatabaseOperation("SELECT", "inventory",
                    "SELECT * FROM inventory WHERE product_id = ?", () -> {
                return inventoryRepository.findById(productId)
                        .orElse(new InventoryEntity(productId, "Product " + productId, 0, 0));
            });
        } catch (Exception e) {
            log.error("Failed to read stock: {}", e.getMessage());
            return new InventoryEntity(productId, "Product " + productId, 0, 0);
        }
    }
}
