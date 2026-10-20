package io.blackbox.sample.notification.controller;

import io.blackbox.sample.common.tracing.TraceContextUtils;
import io.blackbox.sample.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestBody Map<String, Object> body,
                                              @RequestHeader(value = TraceContextUtils.TRACEPARENT_HEADER, required = false) String traceparent) {
        String orderId = String.valueOf(body.getOrDefault("orderId", "ORD-DIRECT"));
        notificationService.processNotification(orderId, body.toString(), TraceContextUtils.getCurrentTraceId(), traceparent);
        return ResponseEntity.ok(Map.of(
                "status", "SENT",
                "orderId", orderId,
                "totalProcessed", notificationService.getProcessedCount()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        return ResponseEntity.ok(Map.of(
                "service", "notification-service",
                "totalProcessed", notificationService.getProcessedCount()
        ));
    }
}
