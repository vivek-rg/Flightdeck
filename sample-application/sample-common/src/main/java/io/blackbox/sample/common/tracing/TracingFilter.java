package io.blackbox.sample.common.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TracingFilter extends OncePerRequestFilter {

    private final Tracer tracer;
    private final OpenTelemetry openTelemetry;

    public TracingFilter(Tracer tracer, OpenTelemetry openTelemetry) {
        this.tracer = tracer;
        this.openTelemetry = openTelemetry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.contains("/actuator") || path.contains("/favicon.ico")) {
            filterChain.doFilter(request, response);
            return;
        }

        Context extractedContext = TraceContextUtils.extractContext(openTelemetry, request);

        String spanName = request.getMethod() + " " + path;
        Span span = tracer.spanBuilder(spanName)
                .setParent(extractedContext)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(AttributeKey.stringKey("http.method"), request.getMethod())
                .setAttribute(AttributeKey.stringKey("http.target"), path)
                .setAttribute(AttributeKey.stringKey("http.route"), path)
                .setAttribute(AttributeKey.stringKey("http.scheme"), request.getScheme())
                .setAttribute(AttributeKey.stringKey("net.host.name"), request.getServerName())
                .startSpan();

        String traceId = span.getSpanContext().getTraceId();
        String spanId = span.getSpanContext().getSpanId();

        response.setHeader(TraceContextUtils.X_TRACE_ID_HEADER, traceId);
        response.setHeader(TraceContextUtils.X_SPAN_ID_HEADER, spanId);

        try (Scope scope = span.makeCurrent()) {
            filterChain.doFilter(request, response);
            int status = response.getStatus();
            span.setAttribute(AttributeKey.longKey("http.status_code"), (long) status);
            if (status >= 400) {
                span.setStatus(StatusCode.ERROR, "HTTP " + status);
            } else {
                span.setStatus(StatusCode.OK);
            }
        } catch (Exception ex) {
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            span.setAttribute(AttributeKey.stringKey("error.type"), ex.getClass().getName());
            throw ex;
        } finally {
            span.end();
        }
    }
}
