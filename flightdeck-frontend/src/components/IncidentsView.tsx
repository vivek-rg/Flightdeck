import React from 'react';
import { AlertTriangle, Clock, ArrowRight, ShieldAlert, Cpu } from 'lucide-react';
import { IncidentItem } from '../types';

interface IncidentsViewProps {
  incidents: IncidentItem[];
  onSelectIncident: (id: string) => void;
}

export const IncidentsView: React.FC<IncidentsViewProps> = ({ incidents, onSelectIncident }) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div className="page-header">
        <div>
          <h1 className="page-title">Incidents & Root-Cause Analysis</h1>
          <p className="page-subtitle">Deterministic correlation of cascading failures, latency spikes, and dependency anomalies</p>
        </div>
      </div>

      <div className="panel">
        <table className="data-table">
          <thead>
            <tr>
              <th>Incident ID</th>
              <th>Trigger Service</th>
              <th>Severity</th>
              <th>Description</th>
              <th>Deterministic Root Cause</th>
              <th>Confidence</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {incidents.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '32px', color: 'var(--text-dim)' }}>
                  No active or past incidents. Use the Failure Simulator to inject an incident!
                </td>
              </tr>
            ) : (
              incidents.map((inc) => (
                <tr key={inc.incidentId}>
                  <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600, color: 'var(--accent-indigo)' }}>
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
                  <td style={{ maxWidth: '280px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {inc.description}
                  </td>
                  <td style={{ fontWeight: 600 }}>
                    {inc.rootCauseCandidate || 'Analyzing...'}
                  </td>
                  <td>
                    {inc.rootCauseConfidence ? (
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <div style={{ width: '50px', height: '6px', background: 'rgba(255,255,255,0.1)', borderRadius: '3px', overflow: 'hidden' }}>
                          <div style={{ width: `${inc.rootCauseConfidence * 100}%`, height: '100%', background: 'var(--accent-indigo)' }} />
                        </div>
                        <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8rem' }}>
                          {Math.round(inc.rootCauseConfidence * 100)}%
                        </span>
                      </div>
                    ) : '--'}
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
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
