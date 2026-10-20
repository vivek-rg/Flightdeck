import React, { useState } from 'react';
import { GitFork, Clock, AlertCircle, ChevronRight, ChevronDown, CheckCircle2, Search } from 'lucide-react';
import { TraceSummary, TraceDetail, TraceNode } from '../types';
import { fetchTraceDetail } from '../api';

interface TraceExplorerProps {
  traces: TraceSummary[];
}

export const TraceExplorerView: React.FC<TraceExplorerProps> = ({ traces }) => {
  const [selectedTraceId, setSelectedTraceId] = useState<string | null>(null);
  const [traceDetail, setTraceDetail] = useState<TraceDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  const handleSelectTrace = async (traceId: string) => {
    setSelectedTraceId(traceId);
    setLoading(true);
    try {
      const data = await fetchTraceDetail(traceId);
      setTraceDetail(data);
    } catch (err) {
      console.error('Failed to load trace detail:', err);
    } finally {
      setLoading(false);
    }
  };

  const filteredTraces = traces.filter(t => 
    t.traceId.toLowerCase().includes(searchTerm.toLowerCase()) ||
    t.rootOperation?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    t.rootServiceName?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div className="page-header">
        <div>
          <h1 className="page-title">Distributed Trace Explorer</h1>
          <p className="page-subtitle">Inspect end-to-end W3C trace propagation, execution waterfalls, and parent-child latency breakdowns</p>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: selectedTraceId ? '1fr 1.3fr' : '1fr', gap: '20px' }}>
        {/* Trace List Panel */}
        <div className="panel">
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
            <Search size={16} color="var(--text-dim)" />
            <input
              type="text"
              placeholder="Search by Trace ID, operation, or service..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{
                flexGrow: 1,
                background: 'var(--bg-card)',
                border: '1px solid var(--border-dim)',
                padding: '8px 12px',
                borderRadius: 'var(--radius-sm)',
                color: 'var(--text-main)',
                fontSize: '0.85rem'
              }}
            />
          </div>

          <table className="data-table">
            <thead>
              <tr>
                <th>Trace ID</th>
                <th>Root Service</th>
                <th>Duration</th>
                <th>Spans</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {filteredTraces.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', color: 'var(--text-dim)', padding: '24px' }}>
                    No traces recorded yet. Run a request from the Simulator to generate distributed traces.
                  </td>
                </tr>
              ) : (
                filteredTraces.map((t) => (
                  <tr
                    key={t.traceId}
                    onClick={() => handleSelectTrace(t.traceId)}
                    style={{
                      cursor: 'pointer',
                      backgroundColor: selectedTraceId === t.traceId ? 'rgba(99, 102, 241, 0.1)' : undefined
                    }}
                  >
                    <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600, color: 'var(--accent-indigo)' }}>
                      {t.traceId.substring(0, 14)}...
                    </td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{t.rootServiceName}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>{t.rootOperation}</div>
                    </td>
                    <td style={{ fontFamily: 'var(--font-mono)' }}>{t.durationMs} ms</td>
                    <td style={{ fontFamily: 'var(--font-mono)' }}>{t.spanCount}</td>
                    <td>
                      <span className={`badge ${t.hasError ? 'badge-critical' : 'badge-success'}`}>
                        {t.hasError ? 'ERROR' : 'OK'}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Selected Trace Reconstructed Waterfall Tree */}
        {selectedTraceId && (
          <div className="panel">
            <div className="panel-header">
              <div>
                <h2 className="panel-title" style={{ fontFamily: 'var(--font-mono)', fontSize: '1rem' }}>
                  Trace: {selectedTraceId}
                </h2>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)', marginTop: '2px' }}>
                  Total Duration: {traceDetail?.durationMs} ms &bull; Spans: {traceDetail?.spanCount} &bull; Errors: {traceDetail?.errorCount}
                </div>
              </div>
              <button className="btn btn-secondary" style={{ padding: '4px 8px', fontSize: '0.75rem' }} onClick={() => setSelectedTraceId(null)}>
                Close
              </button>
            </div>

            {loading ? (
              <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-dim)' }}>Reconstructing trace tree...</div>
            ) : traceDetail ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '520px', overflowY: 'auto' }}>
                {traceDetail.flatSpans.map((span, idx) => {
                  const traceStart = traceDetail.startTime;
                  const traceDuration = Math.max(1, traceDetail.durationMs);
                  const spanOffset = Math.max(0, span.startTime - traceStart);
                  const leftPercent = (spanOffset / traceDuration) * 100;
                  const widthPercent = Math.max(4, (span.durationMs / traceDuration) * 100);

                  return (
                    <div
                      key={span.spanId + idx}
                      style={{
                        padding: '10px 14px',
                        background: 'var(--bg-card)',
                        borderRadius: 'var(--radius-sm)',
                        border: span.error ? '1px solid var(--border-error)' : '1px solid var(--border-dim)',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '6px'
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <span
                            className="badge"
                            style={{
                              background: span.serviceName.includes('payment') ? 'rgba(244,63,94,0.15)' : 'rgba(99,102,241,0.15)',
                              color: span.serviceName.includes('payment') ? '#fb7185' : '#818cf8',
                              fontSize: '0.7rem'
                            }}
                          >
                            {span.serviceName}
                          </span>
                          <span style={{ fontWeight: 600, fontSize: '0.85rem' }}>{span.operationName}</span>
                          <span className="badge badge-info" style={{ fontSize: '0.65rem' }}>{span.spanKind}</span>
                        </div>
                        <div style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8rem', fontWeight: 600, color: span.error ? 'var(--accent-rose)' : 'var(--text-main)' }}>
                          {span.durationMs} ms
                        </div>
                      </div>

                      {/* Waterfall timing visualization */}
                      <div className="waterfall-track">
                        <div
                          className={`waterfall-span ${span.error ? 'error' : ''}`}
                          style={{
                            left: `${leftPercent}%`,
                            width: `${Math.min(100 - leftPercent, widthPercent)}%`
                          }}
                        />
                      </div>

                      {span.statusMessage && (
                        <div style={{ fontSize: '0.78rem', color: '#fb7185', fontFamily: 'var(--font-mono)' }}>
                          &bull; {span.statusMessage}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            ) : null}
          </div>
        )}
      </div>
    </div>
  );
};
