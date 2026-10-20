package io.blackbox.sample.common.tracing;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BlackboxDirectSpanExporter implements SpanExporter {

    private static final Logger log = LoggerFactory.getLogger(BlackboxDirectSpanExporter.class);
    private final String backendIngestUrl;
    private final String serviceName;
    private final HttpClient httpClient;

    public BlackboxDirectSpanExporter(String backendIngestUrl, String serviceName) {
        this.backendIngestUrl = backendIngestUrl;
        this.serviceName = serviceName;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(1000))
                .build();
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        if (spans == null || spans.isEmpty()) {
            return CompletableResultCode.ofSuccess();
        }

        List<Map<String, Object>> dtoList = new ArrayList<>();
        for (SpanData span : spans) {
            Map<String, Object> map = new HashMap<>();
            map.put("traceId", span.getTraceId());
            map.put("spanId", span.getSpanId());
            if (span.getParentSpanContext().isValid()) {
                map.put("parentSpanId", span.getParentSpanContext().getSpanId());
            }
            map.put("serviceName", serviceName);
            map.put("operationName", span.getName());
            map.put("spanKind", span.getKind().name());

            long startMs = span.getStartEpochNanos() / 1_000_000L;
            long endMs = span.getEndEpochNanos() / 1_000_000L;
            map.put("startTime", startMs);
            map.put("endTime", endMs);
            map.put("durationMs", Math.max(1, endMs - startMs));

            map.put("statusCode", span.getStatus().getStatusCode().name());
            if (span.getStatus().getDescription() != null && !span.getStatus().getDescription().isEmpty()) {
                map.put("statusMessage", span.getStatus().getDescription());
            }

            span.getAttributes().forEach((key, value) -> {
                String k = key.getKey();
                if ("http.method".equals(k)) map.put("httpMethod", String.valueOf(value));
                else if ("http.route".equals(k) || "http.target".equals(k)) map.put("httpRoute", String.valueOf(value));
                else if ("peer.service".equals(k)) map.put("peerService", String.valueOf(value));
                else if ("db.system".equals(k)) map.put("dbSystem", String.valueOf(value));
                else if ("messaging.system".equals(k)) map.put("messagingSystem", String.valueOf(value));
            });

            dtoList.add(map);
        }

        CompletableFuture.runAsync(() -> {
            try {
                String json = toJson(dtoList);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(backendIngestUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .timeout(Duration.ofSeconds(2))
                        .build();

                httpClient.sendAsync(req, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                log.debug("Direct span export to {} failed: {}", backendIngestUrl, e.getMessage());
            }
        });

        return CompletableResultCode.ofSuccess();
    }

    private String toJson(List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("{");
            Map<String, Object> map = list.get(i);
            int j = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() == null) continue;
                if (j++ > 0) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                if (entry.getValue() instanceof Number || entry.getValue() instanceof Boolean) {
                    sb.append(entry.getValue());
                } else {
                    sb.append("\"").append(escapeJson(String.valueOf(entry.getValue()))).append("\"");
                }
            }
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
