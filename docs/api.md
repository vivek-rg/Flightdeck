# Flightdeck REST API Documentation

## Base URL
- Flightdeck Core Backend: `http://localhost:8080/api`
- API Gateway (Sample Application): `http://localhost:8081/api`

---

## 1. Authentication
Endpoint: `POST /api/auth/login`
```json
{
  "username": "admin",
  "password": "admin123"
}
```

Pre-configured credentials:
- `admin` / `admin123` (Roles: `ROLE_ADMIN`, `ROLE_ENGINEER`, `ROLE_VIEWER`)
- `engineer` / `engineer123` (Roles: `ROLE_ENGINEER`, `ROLE_VIEWER`)
- `viewer` / `viewer123` (Roles: `ROLE_VIEWER`)

---

## 2. Telemetry Ingest
Endpoint: `POST /api/telemetry/spans`
Supports single `NormalizedSpanDto` or batch array:
```json
[
  {
    "traceId": "abc12345678901234567890123456789",
    "spanId": "span123456789012",
    "parentSpanId": "span000000000000",
    "serviceName": "payment-service",
    "operationName": "REDIS GET payment_lock:101",
    "spanKind": "CLIENT",
    "startTime": 1700000000000,
    "durationMs": 480,
    "statusCode": "OK",
    "dbSystem": "redis",
    "peerService": "redis"
  }
]
```

---

## 3. Discovered Services & Topology
- `GET /api/services` - List all discovered microservices, databases, caches, and brokers.
- `GET /api/dependencies` - Returns graph nodes & edges with call counts, error counts, and average latencies.

---

## 4. Distributed Traces
- `GET /api/traces` - Returns list of recent traces with root operations and error statuses.
- `GET /api/traces/{traceId}` - Returns structured tree waterfall containing spans, child nodes, timing offsets, and attributes.

---

## 5. Incidents & Root-Cause Analysis
- `GET /api/incidents` - List all active and historical incidents.
- `GET /api/incidents/{id}` - Retrieve details of a specific incident.
- `GET /api/incidents/{id}/timeline` - Reconstructed chronological event timeline.
- `GET /api/incidents/{id}/root-cause` - Multi-factor deterministic RCA candidates and scoring breakdown.
- `POST /api/incidents/{id}/explain` - AI narrative explanation strictly interpreting computed RCA evidence.

---

## 6. Incident Replay
- `POST /api/replay/{incidentId}` - Triggers automated controlled replay in sandbox.
- `GET /api/replay/{replayId}` - Retrieves replay run results, comparison report, and reproduction status.

---

## 7. Dashboard & Overview
- `GET /api/dashboard/summary` - Total services, active incidents, error rates, average latencies, and recent incidents.
