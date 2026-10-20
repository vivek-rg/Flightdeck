import React, { useState, useEffect } from 'react';
import { 
  AlertTriangle, 
  Clock, 
  ArrowLeft, 
  PlayCircle, 
  Sparkles, 
  CheckCircle2, 
  ShieldAlert, 
  ListOrdered, 
  Layers, 
  Activity,
  FileText
} from 'lucide-react';
import { IncidentItem, IncidentEvent, RootCauseCandidate } from '../types';
import { fetchIncident, fetchIncidentTimeline, fetchRootCause, fetchLlmExplanation, triggerReplay } from '../api';

interface IncidentDetailViewProps {
  incidentId: string;
  onBack: () => void;
  onNavigateToReplay: (replayId?: string) => void;
}

export const IncidentDetailView: React.FC<IncidentDetailViewProps> = ({ incidentId, onBack, onNavigateToReplay }) => {
  const [incident, setIncident] = useState<IncidentItem | null>(null);
  const [timeline, setTimeline] = useState<IncidentEvent[]>([]);
  const [candidates, setCandidates] = useState<RootCauseCandidate[]>([]);
  const [llmExplanation, setLlmExplanation] = useState<any>(null);
  const [loadingLlm, setLoadingLlm] = useState(false);
  const [replaying, setReplaying] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadIncidentData();
  }, [incidentId]);

  const loadIncidentData = async () => {
    setLoading(true);
    try {
      const [inc, events, rca] = await Promise.all([
        fetchIncident(incidentId),
        fetchIncidentTimeline(incidentId),
        fetchRootCause(incidentId)
      ]);
      setIncident(inc);
      setTimeline(events);
      setCandidates(rca);
    } catch (err) {
      console.error('Failed to load incident details:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateLlm = async () => {
    setLoadingLlm(true);
    try {
      const exp = await fetchLlmExplanation(incidentId);
      setLlmExplanation(exp);
    } catch (err) {
      console.error('Failed to generate LLM explanation:', err);
    } finally {
      setLoadingLlm(false);
    }
  };

  const handleExecuteReplay = async () => {
    setReplaying(true);
    try {
      const result = await triggerReplay(incidentId);
      onNavigateToReplay(result.replayId);
    } catch (err) {
      console.error('Failed to execute replay:', err);
      alert('Replay execution failed. Check console for details.');
    } finally {
      setReplaying(false);
    }
  };

  if (loading) {
    return <div style={{ padding: '60px', textAlign: 'center', color: 'var(--text-dim)' }}>Reconstructing incident timeline and RCA...</div>;
  }

  const primaryCandidate = candidates.find(c => c.primaryRootCause) || candidates[0];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Top Header Navigation */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <button className="btn btn-secondary" onClick={onBack}>
          <ArrowLeft size={16} /> Back to Incidents
        </button>

        <div style={{ display: 'flex', gap: '12px' }}>
          <button className="btn btn-secondary" onClick={handleGenerateLlm} disabled={loadingLlm}>
            <Sparkles size={16} color="var(--accent-purple)" />
            {loadingLlm ? 'Consulting LLM...' : 'Generate AI Explanation'}
          </button>
          <button className="btn btn-primary" onClick={handleExecuteReplay} disabled={replaying}>
            <PlayCircle size={16} />
            {replaying ? 'Executing Replay...' : 'Replay Incident in Sandbox'}
          </button>
        </div>
      </div>

      {/* Incident Status Banner */}
      <div className="panel" style={{ borderLeft: '4px solid var(--accent-rose)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <h1 style={{ fontSize: '1.4rem', fontWeight: 700, fontFamily: 'var(--font-mono)' }}>{incident?.incidentId}</h1>
              <span className={`badge ${incident?.severity === 'CRITICAL' ? 'badge-critical' : 'badge-high'}`}>
                {incident?.severity}
              </span>
              <span className={`badge ${incident?.status === 'ACTIVE' ? 'badge-critical' : 'badge-success'}`}>
                {incident?.status}
              </span>
            </div>
            <p style={{ marginTop: '8px', color: 'var(--text-muted)', fontSize: '0.92rem' }}>
              {incident?.description}
            </p>
          </div>
          <div style={{ textAlign: 'right', fontSize: '0.8rem', color: 'var(--text-dim)' }}>
            <div>Origin Service: <strong style={{ color: 'var(--text-main)' }}>{incident?.triggerService}</strong></div>
            <div>Detected: {incident?.startTime ? new Date(incident.startTime).toLocaleTimeString() : '--'}</div>
          </div>
        </div>
      </div>

      {/* Grid: Chronological Timeline on left, Deterministic RCA Engine on right */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.1fr 1.2fr', gap: '24px' }}>
        {/* Timeline Panel */}
        <div className="panel">
          <div className="panel-header">
            <div>
              <h2 className="panel-title" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <ListOrdered size={18} color="var(--accent-cyan)" /> Reconstructed Timeline
              </h2>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>
                Ordered sequence of telemetry anomalies and cascading milestones
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', position: 'relative' }}>
            {timeline.map((evt, idx) => {
              const timeStr = new Date(evt.timestamp).toTimeString().substring(0, 8);
              const isRootCausePoint = evt.rootCausePoint || evt.eventType.includes('REDIS');

              return (
                <div
                  key={evt.id || idx}
                  style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '14px',
                    padding: '12px',
                    background: isRootCausePoint ? 'rgba(244, 63, 94, 0.08)' : 'var(--bg-card)',
                    border: isRootCausePoint ? '1px solid rgba(244, 63, 94, 0.4)' : '1px solid var(--border-dim)',
                    borderRadius: 'var(--radius-sm)'
                  }}
                >
                  <div style={{ fontFamily: 'var(--font-mono)', fontSize: '0.78rem', color: 'var(--text-dim)', paddingTop: '2px', minWidth: '60px' }}>
                    {timeStr}
                  </div>
                  <div style={{ flexGrow: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                      <span className="badge badge-info" style={{ fontSize: '0.68rem', padding: '1px 6px' }}>
                        {evt.serviceName}
                      </span>
                      <strong style={{ fontSize: '0.85rem', color: isRootCausePoint ? '#fb7185' : 'var(--text-main)' }}>
                        {evt.eventType}
                      </strong>
                      {isRootCausePoint && (
                        <span className="badge badge-critical" style={{ fontSize: '0.65rem' }}>
                          ROOT CAUSE POINT
                        </span>
                      )}
                    </div>
                    <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>{evt.description}</p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Deterministic RCA Candidate Cards */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {primaryCandidate && (
            <div className="panel" style={{ border: '2px solid var(--accent-indigo)', background: 'linear-gradient(180deg, #131b38, #0e162b)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <span className="badge badge-critical" style={{ padding: '4px 10px' }}>
                  DETERMINISTIC PRIMARY ROOT CAUSE
                </span>
                <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.2rem', fontWeight: 800, color: 'var(--accent-indigo)' }}>
                  {Math.round(primaryCandidate.confidence * 100)}% Confidence
                </div>
              </div>

              <h2 style={{ fontSize: '1.3rem', fontWeight: 700, marginBottom: '6px' }}>
                {primaryCandidate.candidateName}
              </h2>
              <div style={{ fontSize: '0.82rem', color: 'var(--text-dim)', marginBottom: '16px' }}>
                Origin: <strong style={{ color: 'var(--text-main)' }}>{primaryCandidate.serviceName}</strong> &bull; Total RCA Score: {primaryCandidate.score}/100
              </div>

              {/* 6-Factor Scoring Breakdown */}
              <div style={{ background: 'rgba(0,0,0,0.3)', padding: '14px', borderRadius: 'var(--radius-sm)', marginBottom: '16px' }}>
                <div style={{ fontSize: '0.78rem', textTransform: 'uppercase', color: 'var(--text-dim)', fontWeight: 700, marginBottom: '10px' }}>
                  Multi-Factor Deterministic Evidence Weights
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', fontSize: '0.8rem' }}>
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '3px' }}>
                      <span>Temporal Correlation</span>
                      <strong>{primaryCandidate.temporalScore}/25</strong>
                    </div>
                    <div style={{ height: '4px', background: 'rgba(255,255,255,0.1)', borderRadius: '2px' }}>
                      <div style={{ width: `${(primaryCandidate.temporalScore / 25) * 100}%`, height: '100%', background: 'var(--accent-cyan)' }} />
                    </div>
                  </div>

                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '3px' }}>
                      <span>Dependency Relationship</span>
                      <strong>{primaryCandidate.dependencyScore}/20</strong>
                    </div>
                    <div style={{ height: '4px', background: 'rgba(255,255,255,0.1)', borderRadius: '2px' }}>
                      <div style={{ width: `${(primaryCandidate.dependencyScore / 20) * 100}%`, height: '100%', background: 'var(--accent-indigo)' }} />
                    </div>
                  </div>

                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '3px' }}>
                      <span>Anomaly Severity (z-score)</span>
                      <strong>{primaryCandidate.anomalyScore}/20</strong>
                    </div>
                    <div style={{ height: '4px', background: 'rgba(255,255,255,0.1)', borderRadius: '2px' }}>
                      <div style={{ width: `${(primaryCandidate.anomalyScore / 20) * 100}%`, height: '100%', background: 'var(--accent-rose)' }} />
                    </div>
                  </div>

                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '3px' }}>
                      <span>Error Propagation</span>
                      <strong>{primaryCandidate.propagationScore}/15</strong>
                    </div>
                    <div style={{ height: '4px', background: 'rgba(255,255,255,0.1)', borderRadius: '2px' }}>
                      <div style={{ width: `${(primaryCandidate.propagationScore / 15) * 100}%`, height: '100%', background: 'var(--accent-purple)' }} />
                    </div>
                  </div>

                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '3px' }}>
                      <span>Retry Correlation</span>
                      <strong>{primaryCandidate.retryScore}/10</strong>
                    </div>
                    <div style={{ height: '4px', background: 'rgba(255,255,255,0.1)', borderRadius: '2px' }}>
                      <div style={{ width: `${(primaryCandidate.retryScore / 10) * 100}%`, height: '100%', background: 'var(--accent-amber)' }} />
                    </div>
                  </div>

                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '3px' }}>
                      <span>Downstream Impact</span>
                      <strong>{primaryCandidate.impactScore}/10</strong>
                    </div>
                    <div style={{ height: '4px', background: 'rgba(255,255,255,0.1)', borderRadius: '2px' }}>
                      <div style={{ width: `${(primaryCandidate.impactScore / 10) * 100}%`, height: '100%', background: 'var(--accent-emerald)' }} />
                    </div>
                  </div>
                </div>
              </div>

              {/* Supporting Evidence List */}
              <div style={{ marginBottom: '14px' }}>
                <h4 style={{ fontSize: '0.85rem', fontWeight: 600, marginBottom: '8px' }}>Supporting Empirical Evidence:</h4>
                <ul style={{ paddingLeft: '18px', fontSize: '0.83rem', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  {primaryCandidate.evidenceJson ? (
                    JSON.parse(primaryCandidate.evidenceJson).map((ev: string, i: number) => (
                      <li key={i}>{ev}</li>
                    ))
                  ) : (
                    <li>Telemetry evidence analyzed deterministically from spans</li>
                  )}
                </ul>
              </div>
            </div>
          )}

          {/* Secondary Symptom Candidates */}
          {candidates.filter(c => !c.primaryRootCause).length > 0 && (
            <div className="panel">
              <h3 style={{ fontSize: '0.95rem', fontWeight: 600, marginBottom: '10px', color: 'var(--text-dim)' }}>
                Downstream Symptoms (Excluded as Root Cause)
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {candidates.filter(c => !c.primaryRootCause).map((c) => (
                  <div key={c.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 14px', background: 'var(--bg-card)', borderRadius: 'var(--radius-sm)' }}>
                    <div>
                      <div style={{ fontWeight: 600, fontSize: '0.85rem' }}>{c.candidateName}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>Identified as downstream symptom (Score: {c.score}/100)</div>
                    </div>
                    <span className="badge badge-info" style={{ fontSize: '0.72rem' }}>SYMPTOM</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* LLM Narrative Explanation Box */}
      {llmExplanation && (
        <div className="panel" style={{ border: '1px solid var(--border-focus)', background: '#10172d' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Sparkles size={20} color="var(--accent-purple)" />
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>AI Root-Cause Explanation</h3>
              <span className="badge badge-info">
                {llmExplanation.llmModelUsed}
              </span>
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>
              Note: LLM strictly explained computed evidence; it did not speculate or alter root-cause.
            </div>
          </div>

          <div style={{ fontSize: '0.88rem', lineHeight: 1.7, whiteSpace: 'pre-line', color: '#e2e8f0', background: 'rgba(0,0,0,0.25)', padding: '18px', borderRadius: 'var(--radius-md)' }}>
            {llmExplanation.explanation}
          </div>
        </div>
      )}
    </div>
  );
};
