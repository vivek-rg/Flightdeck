const API_BASE = '/api';
const GATEWAY_BASE = '/gateway';

const headers = {
  'Content-Type': 'application/json',
  'Authorization': 'Basic ' + btoa('admin:admin123') // Default authorized credentials
};

export async function fetchDashboardSummary() {
  const res = await fetch(`${API_BASE}/dashboard/summary`, { headers });
  if (!res.ok) throw new Error('Failed to load dashboard summary');
  return res.json();
}

export async function fetchServices() {
  const res = await fetch(`${API_BASE}/services`, { headers });
  if (!res.ok) throw new Error('Failed to load services');
  return res.json();
}

export async function fetchDependencies() {
  const res = await fetch(`${API_BASE}/dependencies`, { headers });
  if (!res.ok) throw new Error('Failed to load dependencies');
  return res.json();
}

export async function fetchTraces() {
  const res = await fetch(`${API_BASE}/traces`, { headers });
  if (!res.ok) throw new Error('Failed to load traces');
  return res.json();
}

export async function fetchTraceDetail(traceId: string) {
  const res = await fetch(`${API_BASE}/traces/${traceId}`, { headers });
  if (!res.ok) throw new Error('Failed to load trace detail');
  return res.json();
}

export async function fetchIncidents() {
  const res = await fetch(`${API_BASE}/incidents`, { headers });
  if (!res.ok) throw new Error('Failed to load incidents');
  return res.json();
}

export async function fetchIncident(id: string) {
  const res = await fetch(`${API_BASE}/incidents/${id}`, { headers });
  if (!res.ok) throw new Error('Failed to load incident');
  return res.json();
}

export async function fetchIncidentTimeline(id: string) {
  const res = await fetch(`${API_BASE}/incidents/${id}/timeline`, { headers });
  if (!res.ok) throw new Error('Failed to load incident timeline');
  return res.json();
}

export async function fetchRootCause(id: string) {
  const res = await fetch(`${API_BASE}/incidents/${id}/root-cause`, { headers });
  if (!res.ok) throw new Error('Failed to load root cause analysis');
  return res.json();
}

export async function fetchLlmExplanation(id: string) {
  const res = await fetch(`${API_BASE}/incidents/${id}/explain`, {
    method: 'POST',
    headers
  });
  if (!res.ok) throw new Error('Failed to generate LLM explanation');
  return res.json();
}

export async function fetchAnomalies() {
  const res = await fetch(`${API_BASE}/anomalies`, { headers });
  if (!res.ok) throw new Error('Failed to load anomalies');
  return res.json();
}

export async function triggerReplay(incidentId: string) {
  const res = await fetch(`${API_BASE}/replay/${incidentId}`, {
    method: 'POST',
    headers
  });
  if (!res.ok) throw new Error('Failed to execute incident replay');
  return res.json();
}

export async function fetchReplay(replayId: string) {
  const res = await fetch(`${API_BASE}/replay/${replayId}`, { headers });
  if (!res.ok) throw new Error('Failed to load replay run');
  return res.json();
}

// Interactive failure injection via Gateway
export async function injectFailure(type: string, params: Record<string, string> = {}) {
  const query = new URLSearchParams(params).toString();
  const res = await fetch(`${GATEWAY_BASE}/api/dev/failures/${type}?${query}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  });
  if (!res.ok) throw new Error(`Failure injection failed for ${type}`);
  return res.json();
}

export async function clearFailures() {
  const res = await fetch(`${GATEWAY_BASE}/api/dev/failures/clear`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  });
  if (!res.ok) throw new Error('Failed to clear failures');
  return res.json();
}

export async function createDemoOrder(orderPayload: any) {
  const res = await fetch(`${GATEWAY_BASE}/api/orders`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(orderPayload)
  });
  return {
    status: res.status,
    traceId: res.headers.get('X-Trace-Id'),
    data: await res.json().catch(() => ({}))
  };
}

export async function ingestTelemetrySpans(spans: any[]) {
  const res = await fetch(`${API_BASE}/telemetry/spans`, {
    method: 'POST',
    headers,
    body: JSON.stringify(spans)
  });
  if (!res.ok) throw new Error('Failed to ingest telemetry spans');
  return res.json();
}
