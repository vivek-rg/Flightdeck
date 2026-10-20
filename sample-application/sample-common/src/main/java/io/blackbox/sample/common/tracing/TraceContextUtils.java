package io.blackbox.sample.common.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public final class TraceContextUtils {

    private TraceContextUtils() {}

    public static final String TRACEPARENT_HEADER = "traceparent";
    public static final String TRACESTATE_HEADER = "tracestate";
    public static final String X_TRACE_ID_HEADER = "X-Trace-Id";
    public static final String X_SPAN_ID_HEADER = "X-Span-Id";

    public static final TextMapGetter<HttpServletRequest> HTTP_SERVLET_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(HttpServletRequest carrier) {
            return Collections.list(carrier.getHeaderNames());
        }

        @Override
        public String get(HttpServletRequest carrier, String key) {
            return carrier.getHeader(key);
        }
    };

    public static final TextMapSetter<HttpHeaders> HTTP_HEADERS_SETTER = new TextMapSetter<>() {
        @Override
        public void set(HttpHeaders carrier, String key, String value) {
            if (carrier != null && key != null && value != null) {
                carrier.set(key, value);
            }
        }
    };

    public static final TextMapSetter<Map<String, String>> MAP_SETTER = new TextMapSetter<>() {
        @Override
        public void set(Map<String, String> carrier, String key, String value) {
            if (carrier != null) {
                carrier.put(key, value);
            }
        }
    };

    public static final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier != null ? carrier.get(key) : null;
        }
    };

    public static Context extractContext(OpenTelemetry openTelemetry, HttpServletRequest request) {
        return openTelemetry.getPropagators().getTextMapPropagator().extract(
                Context.current(),
                request,
                HTTP_SERVLET_GETTER
        );
    }

    public static void injectContext(OpenTelemetry openTelemetry, Context context, HttpHeaders headers) {
        openTelemetry.getPropagators().getTextMapPropagator().inject(
                context,
                headers,
                HTTP_HEADERS_SETTER
        );
    }

    public static void injectContext(OpenTelemetry openTelemetry, Context context, Map<String, String> map) {
        openTelemetry.getPropagators().getTextMapPropagator().inject(
                context,
                map,
                MAP_SETTER
        );
    }

    public static String getCurrentTraceId() {
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
            return currentSpan.getSpanContext().getTraceId();
        }
        return null;
    }

    public static String getCurrentSpanId() {
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
            return currentSpan.getSpanContext().getSpanId();
        }
        return null;
    }
}
