package io.blackbox.sample.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.blackbox.sample.common.dto.*;
import io.blackbox.sample.common.tracing.TraceContextUtils;
import io.blackbox.sample.common.tracing.TracingHelper;
import io.blackbox.sample.order.model.OrderEntity;
import io.blackbox.sample.order.model.OrderItemEntity;
import io.blackbox.sample.order.repository.OrderRepository;
import io.opentelemetry.api.trace.Span;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final TracingHelper tracingHelper;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${services.payment-service.url:http://localhost:8083}")
    private String paymentServiceUrl;

    @Value("${services.inventory-service.url:http://localhost:8084}")
    private String inventoryServiceUrl;

    @Value("${blackbox.kafka.orders-topic:orders.created}")
    private String ordersTopic;

    public OrderService(OrderRepository orderRepository,
                        RestTemplate restTemplate,
                        TracingHelper tracingHelper,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
        this.tracingHelper = tracingHelper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderDto createOrder(OrderDto request) {
        String orderId = request.getOrderId() != null ? request.getOrderId() : "ORD-" + UUID.randomUUID().toString().substring(0, 8);
        String traceId = TraceContextUtils.getCurrentTraceId();
        log.info("Creating order: orderId={}, traceId={}", orderId, traceId);

        BigDecimal total = request.getTotalAmount() != null
                ? request.getTotalAmount()
                : (request.getItems() != null
                    ? request.getItems().stream().map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add)
                    : BigDecimal.valueOf(99.99));

        OrderEntity entity = new OrderEntity(
                orderId,
                request.getCustomerId() != null ? request.getCustomerId() : "cust-101",
                total,
                "PENDING_PAYMENT",
                traceId
        );

        if (request.getItems() != null) {
            for (OrderItemDto itemDto : request.getItems()) {
                entity.addItem(new OrderItemEntity(
                        itemDto.getProductId(),
                        itemDto.getQuantity(),
                        itemDto.getPrice()
                ));
            }
        } else {
            entity.addItem(new OrderItemEntity("PROD-1", 1, total));
        }

        try {
            tracingHelper.traceDatabaseOperation("INSERT", "orders", "INSERT INTO orders (order_id, customer_id, total_amount, status) VALUES (?, ?, ?, ?)", () -> {
                return orderRepository.save(entity);
            });
        } catch (Exception e) {
            log.error("Failed to save initial order to database", e);
            throw new RuntimeException("Database error saving order", e);
        }

        // Call Payment Service with Retry pattern
        PaymentResponseDto paymentResponse = processPaymentWithRetries(orderId, entity.getCustomerId(), total, traceId);

        if (!"SUCCESS".equalsIgnoreCase(paymentResponse.getStatus())) {
            entity.setStatus("FAILED");
            entity.setFailureReason("Payment failed: " + paymentResponse.getError());
            try {
                tracingHelper.traceDatabaseOperation("UPDATE", "orders", "UPDATE orders SET status = 'FAILED' WHERE order_id = ?", () -> {
                    return orderRepository.save(entity);
                });
            } catch (Exception ignored) {}

            Span.current().setAttribute("order.status", "FAILED");
            Span.current().setAttribute("order.failure_reason", paymentResponse.getError());
            throw new RuntimeException("Order processing failed: " + paymentResponse.getError());
        }

        // Call Inventory Service to reserve stock
        reserveInventory(orderId, request.getItems(), traceId);

        // Publish OrderCreated event to Kafka
        publishOrderCreatedEvent(entity, request.getItems(), traceId);

        // Mark PAID
        entity.setStatus("PAID");
        try {
            tracingHelper.traceDatabaseOperation("UPDATE", "orders", "UPDATE orders SET status = 'PAID' WHERE order_id = ?", () -> {
                return orderRepository.save(entity);
            });
        } catch (Exception ignored) {}

        return toDto(entity);
    }

    private PaymentResponseDto processPaymentWithRetries(String orderId, String customerId, BigDecimal amount, String traceId) {
        int maxAttempts = 2;
        PaymentResponseDto lastResponse = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("Requesting payment attempt {} of {} for orderId={}", attempt, maxAttempts, orderId);
                PaymentRequestDto payReq = new PaymentRequestDto(orderId, customerId, amount, "CREDIT_CARD");
                payReq.setTraceId(traceId);

                ResponseEntity<PaymentResponseDto> response = restTemplate.postForEntity(
                        paymentServiceUrl + "/payments",
                        payReq,
                        PaymentResponseDto.class
                );

                PaymentResponseDto body = response.getBody();
                if (body != null && "SUCCESS".equalsIgnoreCase(body.getStatus())) {
                    return body;
                }
                lastResponse = body;
            } catch (HttpStatusCodeException ex) {
                log.warn("Payment attempt {} failed with HTTP {}: {}", attempt, ex.getStatusCode(), ex.getResponseBodyAsString());
                lastResponse = new PaymentResponseDto(null, orderId, amount, "FAILED", 0);
                lastResponse.setError("HTTP " + ex.getStatusCode().value() + ": " + ex.getResponseBodyAsString());
            } catch (Exception ex) {
                log.warn("Payment attempt {} failed with exception: {}", attempt, ex.getMessage());
                lastResponse = new PaymentResponseDto(null, orderId, amount, "TIMEOUT", 0);
                lastResponse.setError(ex.getMessage());
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {}
            }
        }

        return lastResponse != null ? lastResponse : new PaymentResponseDto(null, orderId, amount, "FAILED", 0);
    }

    private void reserveInventory(String orderId, List<OrderItemDto> items, String traceId) {
        try {
            InventoryReservationDto resReq = new InventoryReservationDto(
                    orderId,
                    items != null ? items : List.of(new OrderItemDto("PROD-1", 1, BigDecimal.valueOf(99.99))),
                    "RESERVE_REQUESTED",
                    null
            );
            resReq.setTraceId(traceId);

            ResponseEntity<InventoryReservationDto> res = restTemplate.postForEntity(
                    inventoryServiceUrl + "/inventory/reserve",
                    resReq,
                    InventoryReservationDto.class
            );

            if (res.getBody() != null && "FAILED".equalsIgnoreCase(res.getBody().getStatus())) {
                log.error("Inventory reservation returned FAILED: {}", res.getBody().getMessage());
                throw new RuntimeException("Inventory reservation failed: " + res.getBody().getMessage());
            }
        } catch (Exception ex) {
            log.error("Inventory service call failed: {}", ex.getMessage());
            throw new RuntimeException("Inventory service call failed: " + ex.getMessage(), ex);
        }
    }

    private void publishOrderCreatedEvent(OrderEntity order, List<OrderItemDto> items, String traceId) {
        try {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    order.getOrderId(),
                    order.getCustomerId(),
                    order.getTotalAmount(),
                    order.getStatus(),
                    items,
                    traceId
            );
            String payload = objectMapper.writeValueAsString(event);

            tracingHelper.traceKafkaPublish(ordersTopic, order.getOrderId(), () -> {
                if (kafkaTemplate != null) {
                    try {
                        ProducerRecord<String, String> record = new ProducerRecord<>(ordersTopic, order.getOrderId(), payload);
                        if (traceId != null) {
                            record.headers().add("X-Trace-Id", traceId.getBytes(StandardCharsets.UTF_8));
                            String tp = "00-" + traceId + "-" + TraceContextUtils.getCurrentSpanId() + "-01";
                            record.headers().add(TraceContextUtils.TRACEPARENT_HEADER, tp.getBytes(StandardCharsets.UTF_8));
                        }
                        // Asynchronous dispatch so offline Kafka broker does not block HTTP requests
                        new Thread(() -> {
                            try {
                                kafkaTemplate.send(record);
                            } catch (Exception ignored) {}
                        }).start();
                    } catch (Exception e) {
                        log.warn("Could not dispatch event to Kafka: {}", e.getMessage());
                    }
                } else {
                    log.info("KafkaTemplate not configured or offline. Simulated Kafka event published to topic {}: {}", ordersTopic, payload);
                }
                return null;
            });
        } catch (Exception e) {
            log.warn("Could not publish event to Kafka: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(String orderId) {
        OrderEntity entity = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return toDto(entity);
    }

    private OrderDto toDto(OrderEntity entity) {
        List<OrderItemDto> items = entity.getItems().stream()
                .map(i -> new OrderItemDto(i.getProductId(), i.getQuantity(), i.getPrice()))
                .collect(Collectors.toList());
        OrderDto dto = new OrderDto(entity.getOrderId(), entity.getCustomerId(), entity.getTotalAmount(), entity.getStatus(), items);
        dto.setTraceId(entity.getTraceId());
        dto.setFailureReason(entity.getFailureReason());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
