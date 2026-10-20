package io.blackbox.sample.payment.controller;

import io.blackbox.sample.common.dto.PaymentRequestDto;
import io.blackbox.sample.common.dto.PaymentResponseDto;
import io.blackbox.sample.common.tracing.TraceContextUtils;
import io.blackbox.sample.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDto> processPayment(@RequestBody PaymentRequestDto request) {
        PaymentResponseDto response = paymentService.processPayment(request);
        if ("TIMEOUT".equalsIgnoreCase(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .header(TraceContextUtils.X_TRACE_ID_HEADER, TraceContextUtils.getCurrentTraceId())
                    .body(response);
        } else if ("FAILED".equalsIgnoreCase(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(TraceContextUtils.X_TRACE_ID_HEADER, TraceContextUtils.getCurrentTraceId())
                    .body(response);
        }
        return ResponseEntity.ok()
                .header(TraceContextUtils.X_TRACE_ID_HEADER, TraceContextUtils.getCurrentTraceId())
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDto> getPayment(@PathVariable(name = "id") String id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }
}
