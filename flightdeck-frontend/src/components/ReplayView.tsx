import React, { useState, useEffect } from 'react';
import { PlayCircle, CheckCircle2, XCircle, AlertCircle, RefreshCw, ArrowRight } from 'lucide-react';
import { ReplayRun } from '../types';
import { fetchReplay } from '../api';

interface ReplayViewProps {
  initialReplayId?: string;
  onSelectIncident: (id: string) => void;
}

export const ReplayView: React.FC<ReplayViewProps> = ({ initialReplayId, onSelectIncident }) => {
  const [replayIdInput, setReplayIdInput] = useState(initialReplayId || '');
  const [currentReplay, setCurrentReplay] = useState<ReplayRun | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (initialReplayId) {
      loadReplay(initialReplayId);
    }
  }, [initialReplayId]);

  const loadReplay = async (id: string) => {
    setLoading(true);
    try {
      const data = await fetchReplay(id);
      setCurrentReplay(data);
    } catch (err) {
      console.error('Failed to load replay run:', err);
    } finally {
      setLoading(false);
    }
  };

  const failures = currentReplay?.failuresReproducedJson ? JSON.parse(currentReplay.failuresReproducedJson) : [];
  const events = currentReplay?.eventsReproducedJson ? JSON.parse(currentReplay.eventsReproducedJson) : [];
  const diffs = currentReplay?.differencesJson ? JSON.parse(currentReplay.differencesJson) : [];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div className="page-header">
        <div>
          <h1 className="page-title">Controlled Incident Replay & Validation</h1>
          <p className="page-subtitle">
            Deterministically reproduce production failure sequences in sandbox environments and verify telemetry matching
          </p>
        </div>
      </div>

      <div className="panel" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <input
          type="text"
          placeholder="Enter Replay ID (e.g. REP-A1B2C3D4)..."
          value={replayIdInput}
          onChange={(e) => setReplayIdInput(e.target.value)}
          style={{
            flexGrow: 1,
            background: 'var(--bg-card)',
            border: '1px solid var(--border-dim)',
            padding: '10px 14px',
            borderRadius: 'var(--radius-sm)',
            color: 'var(--text-main)',
            fontFamily: 'var(--font-mono)'
          }}
        />
        <button className="btn btn-primary" onClick={() => loadReplay(replayIdInput)} disabled={!replayIdInput || loading}>
          <RefreshCw size={14} /> Load Replay Verification
        </button>
      </div>

      {currentReplay ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {/* Replay Status Card */}
          <div
            className="panel"
            style={{
              borderLeft: `4px solid ${currentReplay.status === 'REPRODUCED' ? 'var(--accent-emerald)' : 'var(--accent-rose)'}`
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <h2 style={{ fontSize: '1.25rem', fontWeight: 700, fontFamily: 'var(--font-mono)' }}>
                    Replay Run: {currentReplay.replayId}
                  </h2>
                  <span className={`badge ${currentReplay.status === 'REPRODUCED' ? 'badge-success' : 'badge-critical'}`}>
                    {currentReplay.status}
                  </span>
                </div>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '6px' }}>
                  {currentReplay.comparisonSummary}
                </p>
              </div>

              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '0.78rem', color: 'var(--text-dim)' }}>Reproduction Match Rate</div>
                <div style={{ fontSize: '1.8rem', fontWeight: 800, fontFamily: 'var(--font-mono)', color: 'var(--accent-emerald)' }}>
                  {Math.round((currentReplay.reproducedRate || 1.0) * 100)}%
                </div>
                <button
                  className="btn btn-secondary"
                  style={{ marginTop: '6px', fontSize: '0.75rem', padding: '4px 10px' }}
                  onClick={() => onSelectIncident(currentReplay.incidentId)}
                >
                  View Original Incident &rarr;
                </button>
              </div>
            </div>
          </div>

          {/* Side-by-side comparison */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
            <div className="panel">
              <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <CheckCircle2 size={18} color="var(--accent-emerald)" /> Injected Test Conditions
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {failures.map((f: string, i: number) => (
                  <div key={i} style={{ padding: '10px 12px', background: 'var(--bg-card)', borderRadius: 'var(--radius-sm)', fontSize: '0.82rem', fontFamily: 'var(--font-mono)' }}>
                    &bull; {f}
                  </div>
                ))}
              </div>
            </div>

            <div className="panel">
              <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <CheckCircle2 size={18} color="var(--accent-cyan)" /> Replayed Milestone Sequence
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {events.map((e: string, i: number) => (
                  <div key={i} style={{ padding: '10px 12px', background: 'var(--bg-card)', borderRadius: 'var(--radius-sm)', fontSize: '0.82rem' }}>
                    &bull; {e}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="panel" style={{ textAlign: 'center', padding: '60px 20px', color: 'var(--text-dim)' }}>
          <PlayCircle size={44} color="var(--accent-indigo)" style={{ marginBottom: '12px' }} />
          <h3>No Replay Run Loaded</h3>
          <p style={{ fontSize: '0.88rem', marginTop: '6px' }}>
            Trigger a replay from an incident details page or run the 1-click failure scenario demonstration!
          </p>
        </div>
      )}
    </div>
  );
};
