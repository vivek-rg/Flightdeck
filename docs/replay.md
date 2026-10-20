# Flightdeck Incident Replay Engine

## 1. Concept & Purpose

Observability without reproducibility is incomplete. When an incident occurs in production, engineers frequently encounter "Cannot Reproduce" (works on my machine) syndrome.

The **Flightdeck Replay Engine**:
1. Analyzes the original incident's telemetry timeline.
2. Extracts critical fault conditions (e.g. Redis latency = 480ms, Payment timeout = true).
3. Injects the precise conditions into the sandbox application.
4. Executes a synthetic request flow mimicking the original incident.
5. Ingests fresh replay telemetry.
6. Evaluates whether the failure chain reproduced identically.

---

## 2. Replay Workflow

```
[ Original Incident ]
       │
       ▼ (Extract Failure Conditions)
[ Replay Scenario Generator ]
       │
       ▼ (POST /api/dev/failures/redis-latency?latencyMs=480)
[ Injected Microservices Environment ]
       │
       ▼ (POST /api/orders)
[ Test Request Execution ]
       │
       ▼ (Collect Fresh Telemetry)
[ Replay Evaluation Engine ]
       │
       ▼ (Compare Replay vs Original)
[ Verdict: REPRODUCED (100% Match Rate) ]
       │
       ▼ (POST /api/dev/failures/clear)
[ Reset Sandbox Environment ]
```

---

## 3. Replay API

### Execute Replay for Incident
`POST /api/replay/{incidentId}`

**Response (HTTP 200)**:
```json
{
  "replayId": "REP-8A2F1B9C",
  "incidentId": "INC-20261005-001",
  "status": "REPRODUCED",
  "reproducedRate": 1.0,
  "comparisonSummary": "Failure chain successfully reproduced in controlled environment. All 4 key incident milestones match original incident telemetry.",
  "failuresReproduced": [
    "INJECTED_CONDITION: Redis latency spike set to 480ms",
    "INJECTED_CONDITION: Payment timeout enabled with 1 simulated retry"
  ],
  "eventsReproduced": [
    "REPLAY_STEP: POST /api/orders (simulated customer order)",
    "REPLAY_STEP: Redis latency spike (480ms) observed in payment-service",
    "REPLAY_STEP: Payment gateway timeout occurred",
    "REPLAY_STEP: Automated retry attempt initiated by order-service",
    "REPLAY_STEP: Final order failure recorded"
  ],
  "differences": []
}
```

### Retrieve Past Replay Run
`GET /api/replay/{replayId}`
