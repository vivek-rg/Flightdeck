package io.blackbox.sample.gateway.controller;

import io.blackbox.sample.common.dto.OrderDto;
import io.blackbox.sample.common.dto.PaymentRequestDto;
import io.blackbox.sample.common.dto.PaymentResponseDto;
import io.blackbox.sample.common.tracing.TraceContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class GatewayRoutingController {

    private static final Logger log = LoggerFactory.getLogger(GatewayRoutingController.class);

    private final RestTemplate restTemplate;

    @Value("${services.order-service.url:http://localhost:8082}")
    private String orderServiceUrl;

    @Value("${services.payment-service.url:http://localhost:8083}")
    private String paymentServiceUrl;

    @Value("${services.inventory-service.url:http://localhost:8084}")
    private String inventoryServiceUrl;

    @Value("${services.notification-service.url:http://localhost:8085}")
    private String notificationServiceUrl;

    public GatewayRoutingController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody OrderDto orderDto) {
        log.info("API Gateway routing POST /orders to {}", orderServiceUrl);
        try {
            ResponseEntity<OrderDto> response = restTemplate.postForEntity(
                    orderServiceUrl + "/orders",
                    orderDto,
                    OrderDto.class
            );
            return ResponseEntity.status(response.getStatusCode())
                    .header(TraceContextUtils.X_TRACE_ID_HEADER, TraceContextUtils.getCurrentTraceId())
                    .body(response.getBody());
        } catch (HttpStatusCodeException ex) {
            log.error("Order service returned HTTP {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Failed to forward createOrder to order-service: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage(), "traceId", String.valueOf(TraceContextUtils.getCurrentTraceId())));
        }
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable(name = "id") String id) {
        log.info("API Gateway routing GET /orders/{} to {}", id, orderServiceUrl);
        try {
            ResponseEntity<OrderDto> response = restTemplate.getForEntity(
                    orderServiceUrl + "/orders/" + id,
                    OrderDto.class
            );
            return ResponseEntity.status(response.getStatusCode())
                    .header(TraceContextUtils.X_TRACE_ID_HEADER, TraceContextUtils.getCurrentTraceId())
                    .body(response.getBody());
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/payments")
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequestDto paymentRequest) {
        log.info("API Gateway routing POST /payments to {}", paymentServiceUrl);
        try {
            ResponseEntity<PaymentResponseDto> response = restTemplate.postForEntity(
                    paymentServiceUrl + "/payments",
                    paymentRequest,
                    PaymentResponseDto.class
            );
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/inventory/{productId}")
    public ResponseEntity<?> getInventory(@PathVariable(name = "productId") String productId) {
        log.info("API Gateway routing GET /inventory/{} to {}", productId, inventoryServiceUrl);
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    inventoryServiceUrl + "/inventory/" + productId,
                    Map.class
            );
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/dev/failures/{failureType}")
    public ResponseEntity<?> routeFailureInjection(@PathVariable(name = "failureType") String failureType,
                                                   @RequestParam Map<String, String> params) {
        log.info("API Gateway routing failure injection: {}", failureType);
        String targetUrl = switch (failureType) {
            case "redis-latency", "redis-unavailable", "payment-timeout" -> paymentServiceUrl;
            case "kafka-delay", "kafka-duplicate" -> notificationServiceUrl;
            case "db-unavailable" -> inventoryServiceUrl;
            default -> orderServiceUrl;
        };

        StringBuilder query = new StringBuilder("?");
        params.forEach((k, v) -> query.append(k).append("=").append(v).append("&"));

        try {
            ResponseEntity<Map> res = restTemplate.postForEntity(
                    targetUrl + "/dev/failures/" + failureType + query,
                    null,
                    Map.class
            );
            return ResponseEntity.ok(res.getBody());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage(), "targetUrl", targetUrl));
        }
    }

    @PostMapping("/dev/failures/clear")
    public ResponseEntity<?> clearAllFailures() {
        log.info("API Gateway broadcasting clear all failures to services");
        String[] services = {orderServiceUrl, paymentServiceUrl, inventoryServiceUrl, notificationServiceUrl};
        for (String s : services) {
            try {
                restTemplate.postForEntity(s + "/dev/failures/clear", null, Map.class);
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.ok(Map.of("status", "CLEARED_ON_ALL_SERVICES"));
    }
}
