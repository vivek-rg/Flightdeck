# Flightdeck

## Distributed-System Failure Reconstruction & Root-Cause Analysis Platform

> **Given telemetry from a distributed application, automatically reconstruct what happened during a failure, discover the dependency chain between services, identify probable root causes using deterministic evidence-based analysis, and replay the incident in a controlled environment.**

---

## 1. Project Overview

**Flightdeck** is an autonomous distributed-systems observability and root-cause analysis platform. Unlike standard log viewers or static dashboards, Flightdeck possesses no hardcoded knowledge of the application topology. It ingests OpenTelemetry spans, dynamically discovers service dependencies, identifies statistical deviations from baselines, isolates cascading failures into unified incidents, calculates mathematical root causes via a 6-factor evidence algorithm, and replays incidents in sandbox environments to confirm reproducibility.

---

## 2. Problem Statement

Modern microservices architectures feature asynchronous communication, message queues, nested caches, and polyglot data stores. When an outage occurs:
1. **Alert Fatigue**: A single backend slowdown (e.g. Redis latency) triggers dozens of alerts across caller services.
2. **Symptom Confusion**: Log aggregators display errors where the failure *manifested* (e.g., `Order Service returned 500`), obscuring where the failure *originated* (e.g., `Redis latency spike` $\to$ `Payment Timeout`).
3. **Cannot Reproduce**: Engineers struggle to recreate ephemeral network delays or transient cache locks in test environments.

---

## 3. Why Existing Logging is Insufficient

| Capability | Traditional Log Aggregation | Distributed Tracing Only | Flightdeck Platform |
| :--- | :--- | :--- | :--- |
| **Cross-Service Context** | Isolated log lines | Visual waterfall | Reconstructed causal tree |
| **Dependency Knowledge** | Manual documentation | Ad-hoc service maps | **Autonomous discovery via spans** |
| **Root Cause Isolation** | "Who logged ERROR last?" | Manual span inspection | **Deterministic 6-factor scoring** |
| **Incident Clustering** | One alert per error | Trace-by-trace | **Sliding-window cascading correlation** |
| **Failure Validation** | Manual reproduction | None | **Automated sandbox replay engine** |

---

## 4. High-Level Architecture

```
                     SAMPLE APPLICATION
            (Gateway, Orders, Payments, Inventory)
                            │
                     OpenTelemetry (W3C traceparent)
                            │
                            ▼
                OpenTelemetry Collector
                            │
                            ▼
                          Kafka (Topic: flightdeck.telemetry)
                            │
                ┌───────────┴───────────┐
                │                       │
                ▼                       ▼
          Event Processor        Metrics Processor
                │                       │
                ▼                       ▼
            PostgreSQL                Redis
                │
                ▼
      Dependency Graph Builder
                │
                ▼
    Incident Reconstruction Engine
                │
                ▼
       Anomaly Detection Engine
                │
                ▼
        Root Cause Engine (Deterministic 6-Factor)
                │
                ▼
          Flightdeck REST API (Spring Boot 3 + Security)
                │
                ▼
          React TypeScript Dashboard
                │
                ▼
     LLM Narrative Layer (Strictly explains precomputed evidence)
```

---

## 5. Technology Choices

- **Backend**: Java 17/21, Spring Boot 3.2.x, Spring Web, Spring Security, Spring Data JPA.
- **Messaging**: Apache Kafka (partition key by `traceId` for causal ordering).
- **Cache**: Redis 7 (idempotency locks, topology cache, recent incident cache).
- **Database**: PostgreSQL 16 (H2 mode for standalone test execution).
- **Distributed Tracing**: OpenTelemetry Java SDK, OpenTelemetry Collector Contrib.
- **Frontend**: React 18, TypeScript, Vite, Lucide Icons, Custom Design Tokens.
- **AI Explanation**: Groq API / Llama 3.1 (strictly receives structured deterministic RCA results; never speculates or alters root cause).

---

## 6. OpenTelemetry & Context Propagation

Every incoming request carries W3C TraceContext headers:
- `traceparent`: `00-${traceId}-${spanId}-${traceFlags}`
- `X-Trace-Id`: Trace identifier propagated to responses.

Spans capture:
- `service.name`: Origin service (`api-gateway`, `order-service`, `payment-service`, `inventory-service`, `notification-service`).
- `span.kind`: `SERVER`, `CLIENT`, `PRODUCER`, `CONSUMER`.
- `peer.service` & `db.system`: Dynamic callee identification (`redis`, `postgresql`, `kafka`).

---

## 7. Kafka Architecture & Topics

| Topic | Partition Key | Purpose |
| :--- | :--- | :--- |
| `flightdeck.telemetry` | `traceId` | Raw/normalized OTLP spans forwarded by OTel Collector |
| `flightdeck.events` | `orderId` | Domain business events (`orders.created`) |
| `flightdeck.incidents` | `incidentId` | Correlated incident notifications |
| `flightdeck.replay` | `replayId` | Controlled replay run executions |

---

## 8. Automatic Dependency Discovery

Flightdeck derives service relationships exclusively from telemetry:
- **HTTP Calls**: Detected when a `CLIENT` span targets `peer.service` or has parent-child linkage.
- **Redis Operations**: Detected when a `CLIENT` span has `db.system = redis`.
- **Database Operations**: Detected when a `CLIENT` span has `db.system = postgresql`.
- **Message Streams**: Detected when `PRODUCER` and `CONSUMER` spans reference `messaging.system = kafka`.

All edges maintain running request counts, error counts, and moving average latencies.

---

## 9. Baseline Anomaly Detection

For every discovered dependency, Flightdeck tracks rolling statistics using Welford's online variance algorithm:
- Normal Redis latency: $\mu \approx 8\text{ ms}, \sigma \approx 3\text{ ms}$.
- When Redis latency increases to $480\text{ ms}$, the deviation multiplier exceeds $60\times$ ($Z > 150$).
- Anomaly Engine emits `REDIS_LATENCY_ANOMALY` with severity `CRITICAL`.

---

## 10. Deterministic Root-Cause Scoring Algorithm

The engine generates multiple candidates and scores each using 6 weighted empirical factors:

$$\text{RootCauseScore} = S_{\text{temporal}} + S_{\text{dependency}} + S_{\text{anomaly}} + S_{\text{propagation}} + S_{\text{retry}} + S_{\text{impact}}$$

1. **Temporal Score ($0-25$)**: Earliest fault in chronological order gets maximum points.
2. **Dependency Score ($0-20$)**: Leaf data stores and upstream dependencies rank higher than orchestrators.
3. **Anomaly Severity ($0-20$)**: Higher standard deviation multipliers receive higher points.
4. **Error Propagation ($0-15$)**: Trace inspection reveals child spans failing caller parent spans.
5. **Retry Correlation ($0-10$)**: Retries triggered downstream following the latency/timeout.
6. **Downstream Impact ($0-10$)**: Blast radius across caller services.

**Root Cause vs Symptom Example**:
- `Redis Latency Spike`: **Score 94.0 / 100 (Confidence: 94%)** $\to$ **PRIMARY ROOT CAUSE**
- `Payment Gateway Timeout`: **Score 58.0 / 100** $\to$ **SYMPTOM**
- `Order Processing Failure`: **Score 34.0 / 100** $\to$ **SYMPTOM**

---

## 11. Controlled Incident Replay

The Replay Engine does not blindly replay production requests:
1. Extracts the exact failure conditions from the incident (`redisLatency = 480ms`, `paymentTimeout = true`).
2. Arms the sandbox environment failure endpoints (`POST /api/dev/failures/redis-latency`).
3. Sends synthetic request through API Gateway.
4. Verifies whether the 4 critical incident milestones reproduced identically:
   - Redis latency spike observed.
   - Payment timeout triggered.
   - Payment retry executed.
   - Order failure recorded.
5. Emits `REPRODUCED` status with 100% reproduction match rate.
6. Automatically cleans up injected failures.

---

## 12. Quick Start & Execution

### Option A: Local Development & Fast Verification (Maven + Node)
```bash
# 1. Build all Java microservices and backend
mvn clean package -DskipTests

# 2. Run unit tests verifying RCA algorithm and dependency discovery
mvn test -pl flightdeck-backend

# 3. Start Backend Core Engine (Port 8080)
java -jar flightdeck-backend/target/flightdeck-backend-1.0.0-SNAPSHOT.jar

# 4. Start React Frontend Dashboard (Port 3000)
cd flightdeck-frontend
npm install
npm run dev
```

### Option B: Full Docker Compose Environment
```bash
docker compose up --build
```
Access points:
- React Dashboard: `http://localhost:3000`
- Flightdeck REST API: `http://localhost:8080/api`
- Sample API Gateway: `http://localhost:8081/api`
- OpenTelemetry Collector: `http://localhost:4318`

---

## 13. Step-by-Step Demonstration Scenario

Follow these steps in the UI Simulator or via terminal:

1. **Send Normal Order**:
   ```bash
   curl -X POST http://localhost:8081/api/orders \
     -H "Content-Type: application/json" \
     -d '{"customerId":"alice","items":[{"productId":"PROD-1","quantity":1,"price":99.99}]}'
   ```
   *Result*: Order succeeds (HTTP 201). Inspect trace and discovered dependencies in UI.

2. **Inject Redis Latency Spike**:
   ```bash
   curl -X POST "http://localhost:8081/api/dev/failures/redis-latency?latencyMs=480"
   curl -X POST "http://localhost:8081/api/dev/failures/payment-timeout?timeout=true&delayMs=2000&failuresCount=1"
   ```

3. **Send Failing Order**:
   ```bash
   curl -X POST http://localhost:8081/api/orders \
     -H "Content-Type: application/json" \
     -d '{"customerId":"bob","items":[{"productId":"PROD-2","quantity":1,"price":149.99}]}'
   ```
   *Result*: Order fails (HTTP 500). Payment timeout and retries triggered.

4. **Observe Flightdeck RCA**:
   - Navigate to `Incidents & RCA`.
   - Open the active incident.
   - Observe chronological timeline from Redis latency spike $\to$ payment timeout $\to$ order failure.
   - Observe `Redis Latency Spike` scored as Primary Root Cause with >85% confidence.

5. **Execute Controlled Replay**:
   - Click **Replay Incident in Sandbox**.
   - Observe replay status `REPRODUCED` with 100% reproduction match rate.

---

## 14. Authentication & Security

Pre-configured credentials:
- `admin` / `admin123` (Role: `ADMIN` - Full Access)
- `engineer` / `engineer123` (Role: `ENGINEER` - Traces, Incidents, Replay)
- `viewer` / `viewer123` (Role: `VIEWER` - Read-Only)
