import React from 'react';
import { Activity, AlertOctagon, CheckCircle2, Clock, Cpu, Server, ArrowRight } from 'lucide-react';
import { IncidentItem } from '../types';

interface OverviewProps {
  summary: any;
  onSelectIncident: (id: string) => void;
  onNavigate: (tab: string) => void;
}

export const OverviewView: React.FC<OverviewProps> = ({ summary, onSelectIncident, onNavigate }) => {
  const recentIncidents: IncidentItem[] = summary?.recentIncidents || [];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <div className="page-header">
        <div>
          <h1 className="page-title">Distributed System Health</h1>
          <p className="page-subtitle">Real-time telemetry ingestion, deterministic failure correlation, and root-cause reconstruction</p>
        </div>
        <button className="btn btn-primary" onClick={() => onNavigate('demo')}>
          Launch Failure Simulation
        </button>
      </div>

      {/* Metrics Row */}
      <div className="metrics-grid">
        <div className="metric-card">
          <div className="metric-label">
            <Server size={16} color="var(--accent-cyan)" />
            Discovered Services
          </div>
          <div className="metric-value" style={{ color: 'var(--accent-cyan)' }}>
            {summary?.totalServices || 5}
          </div>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-dim)' }}>Automatically inferred via OTel</div>
        </div>

        <div className="metric-card">
          <div className="metric-label">
            <AlertOctagon size={16} color="var(--accent-rose)" />
            Active Incidents
          </div>
          <div className="metric-value" style={{ color: summary?.activeIncidents > 0 ? 'var(--accent-rose)' : 'var(--accent-emerald)' }}>
            {summary?.activeIncidents || 0}
          </div>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-dim)' }}>
            {summary?.activeIncidents > 0 ? 'Cascading failures detected' : 'All service clusters nominal'}
          </div>
        </div>

        <div className="metric-card">
          <div className="metric-label">
            <Activity size={16} color="var(--accent-amber)" />
            Telemetry Error Rate
          </div>
          <div className="metric-value" style={{ color: summary?.errorRate > 0 ? 'var(--accent-amber)' : 'var(--accent-emerald)' }}>
            {summary?.errorRate || 0.0}%
          </div>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-dim)' }}>Over recent 1-hour window</div>
        </div>

        <div className="metric-card">
          <div className="metric-label">
            <Clock size={16} color="var(--accent-indigo)" />
            Avg Service Latency
          </div>
          <div className="metric-value" style={{ color: 'var(--accent-indigo)' }}>
            {summary?.averageLatency || 42} ms
          </div>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-dim)' }}>Baseline: 8ms Redis, 12ms DB</div>
        </div>
      </div>

      {/* Recent Incidents Panel */}
      <div className="panel">
        <div className="panel-header">
          <div>
            <h2 className="panel-title">Detected Distributed Incidents</h2>
            <p style={{ fontSize: '0.82rem', color: 'var(--text-dim)', marginTop: '2px' }}>
              Incidents automatically correlated from traces, latency anomalies, and dependency graphs
            </p>
          </div>
          <button className="btn btn-secondary" onClick={() => onNavigate('incidents')}>
            View All Incidents
          </button>
        </div>

        {recentIncidents.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--text-dim)' }}>
            <CheckCircle2 size={36} color="var(--accent-emerald)" style={{ marginBottom: '8px' }} />
            <p>No active incidents recorded. System telemetry is within standard deviation baselines.</p>
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Incident ID</th>
                <th>Trigger Service</th>
                <th>Severity</th>
                <th>Probable Root Cause</th>
                <th>Confidence</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {recentIncidents.map((inc) => (
                <tr key={inc.incidentId}>
                  <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600, color: 'var(--text-main)' }}>
                    {inc.incidentId}
                  </td>
                  <td>
                    <span className="badge badge-info">{inc.triggerService}</span>
                  </td>
                  <td>
                    <span className={`badge ${inc.severity === 'CRITICAL' ? 'badge-critical' : inc.severity === 'HIGH' ? 'badge-high' : 'badge-info'}`}>
                      {inc.severity}
                    </span>
                  </td>
                  <td style={{ fontWeight: 600, color: 'var(--text-main)' }}>
                    {inc.rootCauseCandidate || 'Analyzing telemetry...'}
                  </td>
                  <td>
                    {inc.rootCauseConfidence ? (
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <div style={{ width: '60px', height: '6px', background: 'rgba(255,255,255,0.1)', borderRadius: '3px', overflow: 'hidden' }}>
                          <div style={{ width: `${inc.rootCauseConfidence * 100}%`, height: '100%', background: 'var(--accent-indigo)' }} />
                        </div>
                        <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8rem' }}>
                          {Math.round(inc.rootCauseConfidence * 100)}%
                        </span>
                      </div>
                    ) : (
                      '--'
                    )}
                  </td>
                  <td>
                    <span className={`badge ${inc.status === 'ACTIVE' ? 'badge-critical' : 'badge-success'}`}>
                      {inc.status}
                    </span>
                  </td>
                  <td>
                    <button
                      className="btn btn-secondary"
                      style={{ padding: '6px 12px', fontSize: '0.75rem' }}
                      onClick={() => onSelectIncident(inc.incidentId)}
                    >
                      Investigate <ArrowRight size={14} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Discovered Architectural Topology Banner */}
      <div className="panel" style={{ background: 'linear-gradient(180deg, var(--bg-surface), #111a33)' }}>
        <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '6px' }}>
          Deterministic Discovery Engine
        </h3>
        <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
          Flightdeck discovered service dependencies strictly from observed OpenTelemetry spans. No architecture diagrams or static configurations are pre-loaded.
        </p>
        <div style={{ display: 'flex', gap: '12px', marginTop: '16px' }}>
          <button className="btn btn-secondary" onClick={() => onNavigate('dependencies')}>
            Explore Interactive Dependency Graph &rarr;
          </button>
          <button className="btn btn-secondary" onClick={() => onNavigate('traces')}>
            Inspect Distributed Traces &rarr;
          </button>
        </div>
      </div>
    </div>
  );
};
