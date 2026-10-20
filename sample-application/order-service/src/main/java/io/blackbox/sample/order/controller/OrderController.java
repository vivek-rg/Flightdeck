package io.blackbox.sample.order.controller;

import io.blackbox.sample.common.dto.OrderDto;
import io.blackbox.sample.common.tracing.TraceContextUtils;
import io.blackbox.sample.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderDto request) {
        try {
            OrderDto created = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header(TraceContextUtils.X_TRACE_ID_HEADER, TraceContextUtils.getCurrentTraceId())
                    .body(created);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(TraceContextUtils.X_TRACE_ID_HEADER, TraceContextUtils.getCurrentTraceId())
                    .body(Map.of(
                            "error", ex.getMessage(),
                            "traceId", String.valueOf(TraceContextUtils.getCurrentTraceId()),
                            "status", "FAILED"
                    ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable(name = "id") String id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }
}
