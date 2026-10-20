package io.blackbox.sample.common.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenTelemetryConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenTelemetryConfig.class);

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @Value("${otel.exporter.otlp.endpoint:http://localhost:4318}")
    private String otlpEndpoint;

    @Value("${flightdeck.tracing.enabled:${blackbox.tracing.enabled:true}}")
    private boolean tracingEnabled;

    @Value("${flightdeck.backend.url:${blackbox.backend.url:http://localhost:8080}}")
    private String blackboxBackendUrl;

    @Bean
    public OpenTelemetry openTelemetry() {
        if (!tracingEnabled) {
            log.info("Tracing is disabled. Returning noop OpenTelemetry.");
            return OpenTelemetry.noop();
        }

        Resource serviceResource = Resource.getDefault().merge(
                Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), serviceName,
                        AttributeKey.stringKey("service.version"), "1.0.0"
                ))
        );

        String tracesEndpoint = otlpEndpoint.endsWith("/v1/traces") 
                ? otlpEndpoint 
                : otlpEndpoint + "/v1/traces";

        OtlpHttpSpanExporter spanExporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(tracesEndpoint)
                .setTimeout(Duration.ofSeconds(2))
                .build();

        BlackboxDirectSpanExporter directExporter = new BlackboxDirectSpanExporter(
                blackboxBackendUrl + "/api/telemetry/spans",
                serviceName
        );

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(directExporter))
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter)
                        .setScheduleDelay(Duration.ofMillis(200))
                        .setMaxExportBatchSize(64)
                        .build())
                .setResource(serviceResource)
                .setSampler(Sampler.alwaysOn())
                .build();

        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::close));

        log.info("Initialized OpenTelemetry for service [{}] with OTLP endpoint [{}]", serviceName, tracesEndpoint);
        return sdk;
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(serviceName, "1.0.0");
    }
}
