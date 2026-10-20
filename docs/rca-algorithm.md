# Flightdeck Deterministic Root-Cause Analysis (RCA) Algorithm

## 1. Architectural Philosophy

LLMs are probabilistic and prone to hallucination; they must **never** be tasked with computing root causes in mission-critical distributed systems. 

Flightdeck employs a deterministic, evidence-based algorithm that evaluates telemetry anomalies, dependency graphs, trace propagation trees, and retry loops to produce a mathematical score $S \in [0, 100]$ and confidence rating.

---

## 2. Multi-Factor Scoring Formula

For every candidate root cause $C$ identified during an incident, Flightdeck computes:

$$\text{RootCauseScore}(C) = S_{\text{temporal}} + S_{\text{dependency}} + S_{\text{anomaly}} + S_{\text{propagation}} + S_{\text{retry}} + S_{\text{impact}}$$

Where maximum possible points total **100**:

| Factor | Weight Range | Focus Question |
| :--- | :--- | :--- |
| **Temporal Correlation** ($S_{\text{temporal}}$) | $0 - 25$ pts | Did this fault occur first in the chronological sequence? |
| **Dependency Relationship** ($S_{\text{dependency}}$) | $0 - 20$ pts | Is the candidate a low-level upstream dependency of failing callers? |
| **Anomaly Severity** ($S_{\text{anomaly}}$) | $0 - 20$ pts | How severely did the metric deviate from baseline ($z$-score / multiplier)? |
| **Error Propagation** ($S_{\text{propagation}}$) | $0 - 15$ pts | Did the span error originate here and bubble up to parent spans? |
| **Retry Correlation** ($S_{\text{retry}}$) | $0 - 10$ pts | Did downstream clients initiate retries immediately after this fault? |
| **Downstream Impact** ($S_{\text{impact}}$) | $0 - 10$ pts | How many cascading downstream components failed as a consequence? |

---

## 3. Mathematical Factor Breakdown

### 3.1 Temporal Correlation ($0 - 25$ pts)
The earliest degraded component is favored over downstream victims.
Let $T_{\text{candidate}}$ be the timestamp of candidate's first anomaly and $T_{\text{incident}}$ be the incident start time:
$$\Delta T = T_{\text{candidate}} - T_{\text{incident}}$$
- If $\Delta T \le 1000\text{ms}$: $S_{\text{temporal}} = 25.0$
- If $1000\text{ms} < \Delta T \le 3000\text{ms}$: $S_{\text{temporal}} = 18.0$
- If $\Delta T > 3000\text{ms}$: $S_{\text{temporal}} = 25 \times e^{-\frac{\Delta T}{5000}}$

### 3.2 Dependency Relationship ($0 - 20$ pts)
Using the automatically discovered dependency graph $G = (V, E)$:
- Leaf data stores/caches (e.g. Redis, PostgreSQL) called by failing services receive $20.0$ pts.
- Intermediate microservices (e.g. Payment Service) receive $12.0$ pts.
- Top-level orchestrators (e.g. Order Service, API Gateway) receive $4.0$ pts (classified as victims).

### 3.3 Anomaly Severity ($0 - 20$ pts)
Calculated from observed duration $D_{\text{obs}}$ vs moving baseline $\mu$ and standard deviation $\sigma$:
$$Z = \frac{D_{\text{obs}} - \mu}{\sigma}, \quad \text{Multiplier} = \frac{D_{\text{obs}}}{\mu}$$
- If $\text{Multiplier} \ge 30\times$ (e.g. Redis 480ms vs 8ms): $S_{\text{anomaly}} = 20.0$
- If $5\times \le \text{Multiplier} < 30\times$: $S_{\text{anomaly}} = 14.0$
- Otherwise: $S_{\text{anomaly}} = 8.0$

### 3.4 Error Propagation ($0 - 15$ pts)
Trace hierarchy inspection:
- If child span $s_{\text{child}}$ experienced timeout/error and parent span $s_{\text{parent}}$ subsequently failed with `HTTP 504 Gateway Timeout` or `500 Internal Server Error`, candidate receives $15.0$ pts.

### 3.5 Retry Correlation ($0 - 10$ pts)
- If client microservice (Order Service) executed automated retries following the candidate's latency delay: $S_{\text{retry}} = 10.0$ pts.

### 3.6 Downstream Impact ($0 - 10$ pts)
Calculates cascading blast radius:
- Order failed: $+4.0$
- Inventory update blocked: $+3.0$
- Duplicate messages emitted: $+3.0$

---

## 4. Distinguishing Root Cause from Symptoms

Consider this typical cascading incident:
1. `Redis Latency Spike` $\to$ Score: **94.0 / 100** (Confidence: 94%) $\implies$ **PRIMARY ROOT CAUSE**
2. `Payment Gateway Timeout` $\to$ Score: **58.0 / 100** $\implies$ **SYMPTOM**
3. `Order Service Processing Failure` $\to$ Score: **34.0 / 100** $\implies$ **SYMPTOM**

Flightdeck ranks candidates by score. The highest-scoring candidate is labeled as **PRIMARY ROOT CAUSE** and downstream consequences are tagged as **SYMPTOMS**.
