# Flightdeck Architecture Documentation

## 1. System Mission & Core Principle

Distributed systems do not fail cleanly. In modern cloud-native architectures, failures propagate across asynchronous message boundaries, in-memory caches, and nested database transactions. When an error manifests at the API Gateway or Order Service, traditional monitoring platforms merely display which service logged `HTTP 500`.

**Flightdeck** is an autonomous platform engineered to answer:
- *What happened during the incident?*
- *What services were involved?*
- *What dependency caused the failure?*
- *What deviated from statistical baselines?*
- *What is the mathematical root cause vs downstream symptom?*
- *Can the incident be reproduced deterministically in a sandbox?*

---

## 2. High-Level Telemetry & Data Flow Pipeline

```
                    DISTRIBUTED SAMPLE APPLICATION
         [ API Gateway ]  (Port 8081)
               │ (HTTP + W3C traceparent)
               ▼
         [ Order Service ] (Port 8082)
          ┌────┴──────────────────────────┐
          │ (HTTP RestClient)             │ (Kafka Event: orders.created)
          ▼                               ▼
    [ Payment Service ] (Port 8083)  [ Notification Service ] (Port 8085)
          │                               │
    ┌─────┴─────┐                         │
    ▼           ▼                         │
 [ Redis ]   [ Injected Latency ]         │
                 │                        │
                 ▼                        │
          [ Inventory Service ] (Port 8084)
                 │
                 ▼
          [ PostgreSQL ]
                 │
                 ▼ (OTLP HTTP/gRPC Traces: Port 4318/4317)
      ┌──────────────────────────────────────────────────────────┐
      │           OpenTelemetry Collector Contrib                │
      └──────────────────────────────────────────────────────────┘
                 │                                │
                 ▼ (OTLP JSON)                    ▼ (Direct Fallback Ingest)
         [ Apache Kafka ]               [ REST OTLP Ingest ]
      Topic: flightdeck.telemetry        POST /api/telemetry/spans
                 │                                │
                 └────────────────┬───────────────┘
                                  │
                                  ▼
      ┌──────────────────────────────────────────────────────────┐
      │               FLIGHTDECK BACKEND CORE                    │
      │                                                          │
      │  ┌────────────────────────────────────────────────────┐  │
      │  │ 1. Telemetry Normalizer Engine                     │  │
      │  └────────────────────────────────────────────────────┘  │
      │                           │                              │
      │  ┌────────────────────────┼───────────────────────────┐  │
      │  ▼                        ▼                           ▼  │
      │ [ Dependency Discovery ] [ Anomaly Engine ] [ Trace Recon ]
      │  (Dynamic Topology)       (Online Variance)  (Tree Graph)│
      │                           │                              │
      │                           ▼                              │
      │              [ Incident Detection Engine ]               │
      │               (Sliding Window Grouping)                  │
      │                           │                              │
      │                           ▼                              │
      │           [ Deterministic Root-Cause Engine ]            │
      │            (6-Factor Mathematical Scoring)               │
      │                           │                              │
      │                           ▼                              │
      │               [ Incident Replay Engine ]                 │
      │                (Controlled Environment)                  │
      │                           │                              │
      │                           ▼                              │
      │               [ LLM Explanation Layer ]                  │
      │                (Strictly Explains RCA)                   │
      └──────────────────────────────────────────────────────────┘
                                  │
                                  ▼
                   ┌──────────────────────────────┐
                   │   FLIGHTDECK REST API & UI   │
                   │      (React + TypeScript)    │
                   └──────────────────────────────┘
```

---

## 3. Subsystem Breakdown

### 3.1 Sample E-Commerce Microservices
- **API Gateway (8081)**: Public entry point. Creates root server spans and injects W3C `traceparent` headers to downstream callers.
- **Order Service (8082)**: Order orchestration, relational order/item persistence, payment client with retry pattern, and event producer.
- **Payment Service (8083)**: Idempotency locking in Redis (`SET payment_lock:{id}`), payment execution, and simulated latency injection points.
- **Inventory Service (8084)**: Row-level pessimistic locking (`SELECT FOR UPDATE`) on PostgreSQL inventory rows.
- **Notification Service (8085)**: Kafka consumer with context propagation extraction and simulated queue delays.

### 3.2 Observability Gateway (OpenTelemetry Collector)
- Microservices emit spans using OpenTelemetry Java SDK with OTLP HTTP exporters.
- The OpenTelemetry Collector receives telemetry on `4318`, applies memory limiting and batching, and exports spans to the Kafka topic `flightdeck.telemetry`.

### 3.3 Dynamic Topology Discovery
- No hardcoded topology exists in Flightdeck.
- Flightdeck derives connections by observing:
  - `CLIENT` spans with `peer.service` or `net.peer.name` (HTTP dependencies).
  - `CLIENT` spans with `db.system = redis` or `db.system = postgresql` (Data store dependencies).
  - `PRODUCER` and `CONSUMER` spans with `messaging.system = kafka` (Broker dependencies).
- Real-time call count, moving average latency, and error counts are updated on every span.

### 3.4 Baseline Anomaly Detection
- Maintains online mean ($\mu$) and standard deviation ($\sigma$) via Welford's algorithm for each service dependency.
- Flags statistical deviations where observed duration exceeds $3\sigma$ or $>10\times$ normal baseline (e.g. Redis latency jumping from 8ms to 480ms).

### 3.5 Deterministic Incident Detection & Correlation
- Collects errors and anomalies across services within a 45-second sliding window.
- Clusters correlated cascading failures into a single `IncidentEntity` rather than flooding operators with alert fatigue.
- Generates chronological incident milestones from root span to victim services.
