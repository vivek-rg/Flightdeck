package io.blackbox.sample.notification.service;

import io.blackbox.sample.common.failure.FailureInjectionRegistry;
import io.blackbox.sample.common.tracing.TraceContextUtils;
import io.blackbox.sample.common.tracing.TracingHelper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final TracingHelper tracingHelper;
    private final FailureInjectionRegistry failureRegistry;
    private final OpenTelemetry openTelemetry;
    private final AtomicInteger processedCount = new AtomicInteger(0);

    public NotificationService(TracingHelper tracingHelper,
                               FailureInjectionRegistry failureRegistry,
                               OpenTelemetry openTelemetry) {
        this.tracingHelper = tracingHelper;
        this.failureRegistry = failureRegistry;
        this.openTelemetry = openTelemetry;
    }

    @KafkaListener(topics = "${blackbox.kafka.orders-topic:orders.created}", groupId = "notification-group", autoStartup = "${spring.kafka.consumer.auto-startup:true}")
    public void onOrderCreated(ConsumerRecord<String, String> record) {
        String key = record.key();
        String payload = record.value();

        String traceIdHeader = null;
        if (record.headers().lastHeader("X-Trace-Id") != null) {
            traceIdHeader = new String(record.headers().lastHeader("X-Trace-Id").value(), StandardCharsets.UTF_8);
        }

        String traceparentHeader = null;
        if (record.headers().lastHeader(TraceContextUtils.TRACEPARENT_HEADER) != null) {
            traceparentHeader = new String(record.headers().lastHeader(TraceContextUtils.TRACEPARENT_HEADER).value(), StandardCharsets.UTF_8);
        }

        processNotification(key, payload, traceIdHeader, traceparentHeader);
    }

    public void processNotification(String key, String payload, String traceId, String traceparent) {
        log.info("Processing notification for key={}, traceId={}", key, traceId);

        Context parentContext = Context.current();
        if (traceparent != null && traceparent.startsWith("00-")) {
            String[] parts = traceparent.split("-");
            if (parts.length >= 4) {
                SpanContext remoteCtx = SpanContext.createFromRemoteParent(
                        parts[1], parts[2], TraceFlags.getSampled(), TraceState.getDefault());
                parentContext = Context.current().with(Span.wrap(remoteCtx));
            }
        }

        Span span = tracingHelper.getTracer().spanBuilder("KAFKA process orders.created")
                .setParent(parentContext)
                .setSpanKind(SpanKind.CONSUMER)
                .setAttribute(AttributeKey.stringKey("messaging.system"), "kafka")
                .setAttribute(AttributeKey.stringKey("messaging.destination"), "orders.created")
                .setAttribute(AttributeKey.stringKey("messaging.operation"), "process")
                .setAttribute(AttributeKey.stringKey("peer.service"), "kafka")
                .setAttribute(AttributeKey.stringKey("notification.order_id"), key != null ? key : "unknown")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            long delay = failureRegistry.getKafkaConsumerDelay();
            if (delay > 0) {
                log.warn("FAILURE INJECTION: Kafka consumer delay of {}ms simulated", delay);
                Thread.sleep(delay);
            }

            if (failureRegistry.isKafkaDuplicateEvent()) {
                log.warn("FAILURE INJECTION: Kafka duplicate event processing simulated for order: {}", key);
                span.setAttribute(AttributeKey.booleanKey("messaging.kafka.duplicate"), true);
                processedCount.incrementAndGet();
            }

            processedCount.incrementAndGet();
            log.info("Successfully sent notification for order {}. Total sent: {}", key, processedCount.get());
            span.setStatus(StatusCode.OK);
        } catch (Exception ex) {
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            log.error("Failed to process notification: {}", ex.getMessage());
        } finally {
            span.end();
        }
    }

    public int getProcessedCount() {
        return processedCount.get();
    }
}
