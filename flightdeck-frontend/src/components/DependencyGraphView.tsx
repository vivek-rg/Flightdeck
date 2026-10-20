import React, { useState } from 'react';
import { Network, Database, Cpu, Radio, HardDrive, RefreshCw } from 'lucide-react';
import { DependencyNode, DependencyEdge } from '../types';

interface DependencyGraphProps {
  graphData: {
    nodes: DependencyNode[];
    edges: DependencyEdge[];
    timestamp?: number;
  };
  onRefresh: () => void;
}

export const DependencyGraphView: React.FC<DependencyGraphProps> = ({ graphData, onRefresh }) => {
  const [selectedNode, setSelectedNode] = useState<string | null>(null);

  const nodes = graphData?.nodes || [];
  const edges = graphData?.edges || [];

  // Coordinate layout for known service nodes
  const nodeCoordinates: Record<string, { x: number; y: number; label: string; icon: string }> = {
    'api-gateway': { x: 120, y: 190, label: 'API Gateway', icon: 'gateway' },
    'order-service': { x: 340, y: 190, label: 'Order Service', icon: 'service' },
    'payment-service': { x: 580, y: 100, label: 'Payment Service', icon: 'service' },
    'inventory-service': { x: 580, y: 280, label: 'Inventory Service', icon: 'service' },
    'redis': { x: 800, y: 100, label: 'Redis (Cache / Lock)', icon: 'redis' },
    'postgresql': { x: 800, y: 280, label: 'PostgreSQL (DB)', icon: 'postgres' },
    'kafka': { x: 460, y: 390, label: 'Kafka (Broker)', icon: 'kafka' },
    'notification-service': { x: 740, y: 390, label: 'Notification Service', icon: 'service' }
  };

  const getNodeIcon = (type: string) => {
    switch (type.toLowerCase()) {
      case 'redis': return <HardDrive size={18} color="var(--accent-rose)" />;
      case 'database':
      case 'postgres':
      case 'postgresql': return <Database size={18} color="var(--accent-cyan)" />;
      case 'kafka':
      case 'broker': return <Radio size={18} color="var(--accent-amber)" />;
      default: return <Cpu size={18} color="var(--accent-indigo)" />;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div className="page-header">
        <div>
          <h1 className="page-title">Automatically Discovered Dependency Graph</h1>
          <p className="page-subtitle">
            Derived in real-time from OpenTelemetry spans without hardcoded topologies or static configurations
          </p>
        </div>
        <button className="btn btn-secondary" onClick={onRefresh}>
          <RefreshCw size={14} /> Refresh Graph
        </button>
      </div>

      {/* Graph Visual Canvas */}
      <div className="panel" style={{ padding: '16px', position: 'relative', overflow: 'hidden' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
          <div style={{ display: 'flex', gap: '16px', fontSize: '0.78rem' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ width: '10px', height: '10px', background: 'var(--accent-indigo)', borderRadius: '2px' }} />
              Microservice
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ width: '10px', height: '10px', background: 'var(--accent-rose)', borderRadius: '2px' }} />
              Redis Cache
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ width: '10px', height: '10px', background: 'var(--accent-cyan)', borderRadius: '2px' }} />
              PostgreSQL
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ width: '10px', height: '10px', background: 'var(--accent-amber)', borderRadius: '2px' }} />
              Kafka
            </span>
          </div>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-dim)', fontFamily: 'var(--font-mono)' }}>
            Observed Edges: {edges.length} &bull; Total Nodes: {nodes.length}
          </div>
        </div>

        <svg viewBox="0 0 960 480" style={{ width: '100%', height: 'auto', background: '#0a0e1a', borderRadius: '12px', border: '1px solid var(--border-dim)' }}>
          <defs>
            <marker id="arrow" viewBox="0 0 10 10" refX="28" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
              <path d="M 0 0 L 10 5 L 0 10 z" fill="#6366f1" opacity="0.8" />
            </marker>
            <marker id="arrow-error" viewBox="0 0 10 10" refX="28" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
              <path d="M 0 0 L 10 5 L 0 10 z" fill="#f43f5e" opacity="0.9" />
            </marker>
          </defs>

          {/* Render Graph Edges */}
          {edges.map((edge) => {
            const srcCoord = nodeCoordinates[edge.source] || { x: 200, y: 200 };
            const dstCoord = nodeCoordinates[edge.target] || { x: 400, y: 200 };
            const isError = edge.errorCount > 0;
            const isSelected = selectedNode === edge.source || selectedNode === edge.target;

            const midX = (srcCoord.x + dstCoord.x) / 2;
            const midY = (srcCoord.y + dstCoord.y) / 2;

            return (
              <g key={edge.id}>
                <line
                  x1={srcCoord.x}
                  y1={srcCoord.y}
                  x2={dstCoord.x}
                  y2={dstCoord.y}
                  stroke={isError ? '#f43f5e' : isSelected ? '#a855f7' : '#475569'}
                  strokeWidth={isSelected ? 3 : 2}
                  strokeDasharray={edge.type === 'KAFKA' ? '4 4' : 'none'}
                  markerEnd={isError ? 'url(#arrow-error)' : 'url(#arrow)'}
                />
                <rect
                  x={midX - 32}
                  y={midY - 10}
                  width="64"
                  height="18"
                  rx="4"
                  fill="#111827"
                  stroke={isError ? '#f43f5e' : '#334155'}
                  strokeWidth="1"
                />
                <text
                  x={midX}
                  y={midY + 3}
                  textAnchor="middle"
                  fill={isError ? '#fb7185' : '#94a3b8'}
                  fontSize="9"
                  fontFamily="var(--font-mono)"
                >
                  {edge.averageLatency}ms
                </text>
              </g>
            );
          })}

          {/* Render Graph Nodes */}
          {nodes.map((node) => {
            const coord = nodeCoordinates[node.id] || { x: 300, y: 240, label: node.name, icon: 'service' };
            const isSelected = selectedNode === node.id;

            return (
              <g
                key={node.id}
                transform={`translate(${coord.x}, ${coord.y})`}
                onClick={() => setSelectedNode(selectedNode === node.id ? null : node.id)}
                style={{ cursor: 'pointer' }}
              >
                <circle
                  r={isSelected ? 30 : 25}
                  fill={node.id === 'redis' ? '#27101e' : node.id.includes('postgres') ? '#0c2233' : '#141d38'}
                  stroke={isSelected ? '#a855f7' : node.id === 'redis' ? 'var(--accent-rose)' : node.id.includes('postgres') ? 'var(--accent-cyan)' : 'var(--accent-indigo)'}
                  strokeWidth={isSelected ? 3 : 2}
                />
                <foreignObject x="-12" y="-12" width="24" height="24">
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '100%', height: '100%' }}>
                    {getNodeIcon(node.type || node.id)}
                  </div>
                </foreignObject>
                <text
                  y="42"
                  textAnchor="middle"
                  fill="#f8fafc"
                  fontSize="11"
                  fontWeight="600"
                  fontFamily="var(--font-sans)"
                >
                  {coord.label || node.name}
                </text>
              </g>
            );
          })}
        </svg>
      </div>

      {/* Discovered Edges Details Table */}
      <div className="panel">
        <h2 className="panel-title" style={{ marginBottom: '16px' }}>Observed Telemetry Connections</h2>
        <table className="data-table">
          <thead>
            <tr>
              <th>Source Service</th>
              <th>Destination Service</th>
              <th>Protocol / Type</th>
              <th>Call Count</th>
              <th>Error Count</th>
              <th>Observed Avg Latency</th>
              <th>Topology Status</th>
            </tr>
          </thead>
          <tbody>
            {edges.map((e) => (
              <tr key={e.id}>
                <td style={{ fontWeight: 600, fontFamily: 'var(--font-mono)' }}>{e.source}</td>
                <td style={{ fontWeight: 600, fontFamily: 'var(--font-mono)' }}>{e.target}</td>
                <td>
                  <span className="badge badge-info">{e.type}</span>
                </td>
                <td style={{ fontFamily: 'var(--font-mono)' }}>{e.requestCount}</td>
                <td>
                  {e.errorCount > 0 ? (
                    <span className="badge badge-critical">{e.errorCount} errors</span>
                  ) : (
                    <span style={{ color: 'var(--accent-emerald)' }}>0</span>
                  )}
                </td>
                <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 600 }}>{e.averageLatency} ms</td>
                <td>
                  <span className={`badge ${e.errorCount > 0 ? 'badge-critical' : 'badge-success'}`}>
                    {e.errorCount > 0 ? 'DEGRADED' : 'HEALTHY'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
