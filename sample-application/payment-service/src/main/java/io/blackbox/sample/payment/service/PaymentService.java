package io.blackbox.sample.payment.service;

import io.blackbox.sample.common.dto.PaymentRequestDto;
import io.blackbox.sample.common.dto.PaymentResponseDto;
import io.blackbox.sample.common.failure.FailureInjectionRegistry;
import io.blackbox.sample.common.tracing.TraceContextUtils;
import io.blackbox.sample.common.tracing.TracingHelper;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final TracingHelper tracingHelper;
    private final FailureInjectionRegistry failureRegistry;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // In-memory fallback if Redis broker is offline in local dev
    private final Map<String, String> localMemoryCache = new ConcurrentHashMap<>();

    public PaymentService(TracingHelper tracingHelper, FailureInjectionRegistry failureRegistry) {
        this.tracingHelper = tracingHelper;
        this.failureRegistry = failureRegistry;
    }

    public PaymentResponseDto processPayment(PaymentRequestDto request) {
        long startTime = System.currentTimeMillis();
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
        String traceId = TraceContextUtils.getCurrentTraceId();
        String lockKey = "payment_lock:" + request.getOrderId();

        log.info("Processing payment: paymentId={}, orderId={}, amount={}, traceId={}",
                paymentId, request.getOrderId(), request.getAmount(), traceId);

        // 1. Redis Operation: Acquire payment lock / idempotency check
        try {
            tracingHelper.traceRedisOperation("GET", lockKey, () -> {
                // Check injected Redis failures
                if (failureRegistry.isRedisUnavailable()) {
                    log.error("FAILURE INJECTION: Redis connection failure simulated");
                    throw new RuntimeException("RedisConnectionFailureException: Unable to connect to Redis on redis:6379");
                }

                long latency = failureRegistry.getRedisLatency();
                if (latency > 0) {
                    log.warn("FAILURE INJECTION: Injecting Redis latency delay of {}ms", latency);
                    Thread.sleep(latency);
                } else {
                    // Normal simulated Redis latency (e.g. 5-10ms)
                    Thread.sleep(8);
                }

                if (redisTemplate != null) {
                    try {
                        return redisTemplate.opsForValue().get(lockKey);
                    } catch (Exception ex) {
                        log.warn("Real Redis failed, falling back to local memory store: {}", ex.getMessage());
                        return localMemoryCache.get(lockKey);
                    }
                } else {
                    return localMemoryCache.get(lockKey);
                }
            });
        } catch (Exception ex) {
            log.error("Redis operation failed during payment check: {}", ex.getMessage());
            Span.current().setStatus(StatusCode.ERROR, "Redis operation failed: " + ex.getMessage());
            Span.current().setAttribute(AttributeKey.stringKey("error.cause"), "REDIS_FAILURE");
            PaymentResponseDto failed = new PaymentResponseDto(paymentId, request.getOrderId(), request.getAmount(), "FAILED", System.currentTimeMillis() - startTime);
            failed.setError("Redis failure: " + ex.getMessage());
            failed.setTraceId(traceId);
            return failed;
        }

        // 2. Simulated Payment Gateway execution / timeout
        if (failureRegistry.isPaymentTimeout() || failureRegistry.getPaymentDelay() > 0) {
            long delay = failureRegistry.getPaymentDelay() > 0 ? failureRegistry.getPaymentDelay() : 2500;
            boolean shouldFail = failureRegistry.decrementAndCheckPaymentFailure();
            log.warn("Payment delay injected: delay={}ms, shouldFailAfterDelay={}", delay, shouldFail);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ignored) {}

            if (shouldFail) {
                log.error("FAILURE INJECTION: Simulated payment gateway timeout for orderId={}", request.getOrderId());
                Span.current().setStatus(StatusCode.ERROR, "Payment gateway timeout");
                Span.current().setAttribute(AttributeKey.stringKey("error.cause"), "PAYMENT_GATEWAY_TIMEOUT");
                PaymentResponseDto timeoutResponse = new PaymentResponseDto(paymentId, request.getOrderId(), request.getAmount(), "TIMEOUT", System.currentTimeMillis() - startTime);
                timeoutResponse.setError("Payment gateway timeout after " + delay + "ms");
                timeoutResponse.setTraceId(traceId);
                return timeoutResponse;
            }
        }

        // 3. Redis Operation: Save payment status
        String statusKey = "payment:" + paymentId;
        try {
            tracingHelper.traceRedisOperation("SET", statusKey, () -> {
                long latency = failureRegistry.getRedisLatency();
                if (latency > 0) {
                    Thread.sleep(latency);
                } else {
                    Thread.sleep(8);
                }

                if (redisTemplate != null) {
                    try {
                        redisTemplate.opsForValue().set(statusKey, "SUCCESS", 1, TimeUnit.HOURS);
                        redisTemplate.opsForValue().set(lockKey, paymentId, 1, TimeUnit.HOURS);
                    } catch (Exception e) {
                        localMemoryCache.put(statusKey, "SUCCESS");
                        localMemoryCache.put(lockKey, paymentId);
                    }
                } else {
                    localMemoryCache.put(statusKey, "SUCCESS");
                    localMemoryCache.put(lockKey, paymentId);
                }
                return "OK";
            });
        } catch (Exception ex) {
            log.error("Redis operation failed during payment commit: {}", ex.getMessage());
        }

        long totalLatency = System.currentTimeMillis() - startTime;
        PaymentResponseDto response = new PaymentResponseDto(paymentId, request.getOrderId(), request.getAmount(), "SUCCESS", totalLatency);
        response.setTraceId(traceId);
        return response;
    }

    public PaymentResponseDto getPayment(String paymentId) {
        String statusKey = "payment:" + paymentId;
        String status = "NOT_FOUND";
        try {
            status = tracingHelper.traceRedisOperation("GET", statusKey, () -> {
                if (redisTemplate != null) {
                    try {
                        String s = redisTemplate.opsForValue().get(statusKey);
                        return s != null ? s : localMemoryCache.getOrDefault(statusKey, "NOT_FOUND");
                    } catch (Exception e) {
                        return localMemoryCache.getOrDefault(statusKey, "NOT_FOUND");
                    }
                }
                return localMemoryCache.getOrDefault(statusKey, "NOT_FOUND");
            });
        } catch (Exception ignored) {}

        PaymentResponseDto dto = new PaymentResponseDto(paymentId, "UNKNOWN", null, status, 5);
        dto.setTraceId(TraceContextUtils.getCurrentTraceId());
        return dto;
    }
}
