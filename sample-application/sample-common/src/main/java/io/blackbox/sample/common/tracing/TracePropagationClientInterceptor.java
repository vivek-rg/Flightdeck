package io.blackbox.sample.common.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TracePropagationClientInterceptor implements ClientHttpRequestInterceptor {

    private final Tracer tracer;
    private final OpenTelemetry openTelemetry;

    public TracePropagationClientInterceptor(Tracer tracer, OpenTelemetry openTelemetry) {
        this.tracer = tracer;
        this.openTelemetry = openTelemetry;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        String spanName = "HTTP " + request.getMethod() + " " + request.getURI().getHost();
        String host = request.getURI().getHost();
        int port = request.getURI().getPort();

        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(AttributeKey.stringKey("http.method"), request.getMethod().name())
                .setAttribute(AttributeKey.stringKey("http.url"), request.getURI().toString())
                .setAttribute(AttributeKey.stringKey("peer.service"), host)
                .setAttribute(AttributeKey.stringKey("net.peer.name"), host)
                .setAttribute(AttributeKey.longKey("net.peer.port"), (long) (port > 0 ? port : 80))
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            TraceContextUtils.injectContext(openTelemetry, Context.current(), request.getHeaders());
            ClientHttpResponse response = execution.execute(request, body);
            span.setAttribute(AttributeKey.longKey("http.status_code"), (long) response.getStatusCode().value());
            if (response.getStatusCode().isError()) {
                span.setStatus(StatusCode.ERROR, "HTTP " + response.getStatusCode().value());
            } else {
                span.setStatus(StatusCode.OK);
            }
            return response;
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
