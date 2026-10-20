package io.blackbox.sample.common.tracing;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Component
public class TracingHelper {

    private final Tracer tracer;

    public TracingHelper(Tracer tracer) {
        this.tracer = tracer;
    }

    public <T> T traceRedisOperation(String operation, String key, Callable<T> action) throws Exception {
        Span span = tracer.spanBuilder("REDIS " + operation)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(AttributeKey.stringKey("db.system"), "redis")
                .setAttribute(AttributeKey.stringKey("db.operation"), operation)
                .setAttribute(AttributeKey.stringKey("db.redis.key"), key)
                .setAttribute(AttributeKey.stringKey("peer.service"), "redis")
                .setAttribute(AttributeKey.stringKey("net.peer.name"), "redis")
                .setAttribute(AttributeKey.longKey("net.peer.port"), 6379L)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            T result = action.call();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception ex) {
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            span.setAttribute(AttributeKey.stringKey("error.type"), ex.getClass().getName());
            throw ex;
        } finally {
            span.end();
        }
    }

    public <T> T traceDatabaseOperation(String operation, String table, String sql, Callable<T> action) throws Exception {
        Span span = tracer.spanBuilder("DB " + operation + " " + table)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(AttributeKey.stringKey("db.system"), "postgresql")
                .setAttribute(AttributeKey.stringKey("db.operation"), operation)
                .setAttribute(AttributeKey.stringKey("db.name"), "flightdeck_sample")
                .setAttribute(AttributeKey.stringKey("db.sql.table"), table)
                .setAttribute(AttributeKey.stringKey("db.statement"), sql)
                .setAttribute(AttributeKey.stringKey("peer.service"), "postgresql")
                .setAttribute(AttributeKey.stringKey("net.peer.name"), "postgresql")
                .setAttribute(AttributeKey.longKey("net.peer.port"), 5432L)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            T result = action.call();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception ex) {
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            span.setAttribute(AttributeKey.stringKey("error.type"), ex.getClass().getName());
            throw ex;
        } finally {
            span.end();
        }
    }

    public <T> T traceKafkaPublish(String topic, String key, Callable<T> action) throws Exception {
        Span span = tracer.spanBuilder("KAFKA send " + topic)
                .setSpanKind(SpanKind.PRODUCER)
                .setAttribute(AttributeKey.stringKey("messaging.system"), "kafka")
                .setAttribute(AttributeKey.stringKey("messaging.destination"), topic)
                .setAttribute(AttributeKey.stringKey("messaging.destination_kind"), "topic")
                .setAttribute(AttributeKey.stringKey("messaging.kafka.message_key"), key)
                .setAttribute(AttributeKey.stringKey("peer.service"), "kafka")
                .setAttribute(AttributeKey.stringKey("net.peer.name"), "kafka")
                .setAttribute(AttributeKey.longKey("net.peer.port"), 9092L)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            T result = action.call();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception ex) {
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            span.setAttribute(AttributeKey.stringKey("error.type"), ex.getClass().getName());
            throw ex;
        } finally {
            span.end();
        }
    }

    public Tracer getTracer() {
        return tracer;
    }
}
