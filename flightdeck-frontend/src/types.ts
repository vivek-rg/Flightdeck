export interface ServiceItem {
  id: number;
  serviceName: string;
  serviceType: string;
  firstSeen: number;
  lastSeen: number;
  status: string;
}

export interface DependencyNode {
  id: string;
  name: string;
  type: string;
  status: string;
}

export interface DependencyEdge {
  id: string;
  source: string;
  target: string;
  type: string;
  requestCount: number;
  errorCount: number;
  averageLatency: number;
}

export interface TraceSummary {
  traceId: string;
  rootServiceName: string;
  rootOperation: string;
  startTime: number;
  endTime: number;
  durationMs: number;
  spanCount: number;
  hasError: boolean;
  errorCount: number;
  servicesInvolved: string;
}

export interface TraceNode {
  spanId: string;
  parentSpanId?: string;
  serviceName: string;
  operationName: string;
  spanKind: string;
  startTime: number;
  endTime: number;
  durationMs: number;
  statusCode: string;
  statusMessage?: string;
  error: boolean;
  peerService?: string;
  dbSystem?: string;
  messagingSystem?: string;
  children: TraceNode[];
}

export interface TraceDetail {
  traceId: string;
  rootServiceName: string;
  rootOperation: string;
  startTime: number;
  endTime: number;
  durationMs: number;
  spanCount: number;
  hasError: boolean;
  errorCount: number;
  servicesInvolved: string[];
  rootNode: TraceNode;
  flatSpans: TraceNode[];
}

export interface IncidentItem {
  incidentId: string;
  startTime: number;
  endTime?: number;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  status: 'ACTIVE' | 'RESOLVED' | 'REPLAYED';
  triggerService: string;
  description: string;
  rootCauseCandidate?: string;
  rootCauseConfidence?: number;
}

export interface IncidentEvent {
  id: number;
  timestamp: number;
  serviceName: string;
  eventType: string;
  description: string;
  traceId?: string;
  spanId?: string;
  rootCausePoint: boolean;
}

export interface RootCauseCandidate {
  id: number;
  incidentId: string;
  candidateName: string;
  serviceName: string;
  score: number;
  confidence: number;
  temporalScore: number;
  dependencyScore: number;
  anomalyScore: number;
  propagationScore: number;
  retryScore: number;
  impactScore: number;
  primaryRootCause: boolean;
  evidenceJson?: string;
  affectedServicesJson?: string;
  downstreamEffectsJson?: string;
}

export interface ReplayRun {
  replayId: string;
  incidentId: string;
  startTime: number;
  endTime?: number;
  status: 'PENDING' | 'RUNNING' | 'REPRODUCED' | 'FAILED';
  reproducedRate: number;
  failuresReproducedJson?: string;
  eventsReproducedJson?: string;
  differencesJson?: string;
  comparisonSummary?: string;
}
