import React from 'react';
import { Layers, Server, HardDrive, Database, Radio, RefreshCw } from 'lucide-react';
import { ServiceItem } from '../types';

interface ServicesViewProps {
  services: ServiceItem[];
  onRefresh: () => void;
}

export const ServicesView: React.FC<ServicesViewProps> = ({ services, onRefresh }) => {
  const getIcon = (type: string) => {
    switch (type?.toLowerCase()) {
      case 'redis':
      case 'cache': return <HardDrive size={18} color="var(--accent-rose)" />;
      case 'database':
      case 'postgresql': return <Database size={18} color="var(--accent-cyan)" />;
      case 'broker':
      case 'kafka': return <Radio size={18} color="var(--accent-amber)" />;
      default: return <Server size={18} color="var(--accent-indigo)" />;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div className="page-header">
        <div>
          <h1 className="page-title">Service Catalogue</h1>
          <p className="page-subtitle">Discovered distributed components, data stores, and message brokers</p>
        </div>
        <button className="btn btn-secondary" onClick={onRefresh}>
          <RefreshCw size={14} /> Refresh Catalogue
        </button>
      </div>

      <div className="panel">
        <table className="data-table">
          <thead>
            <tr>
              <th>Service Name</th>
              <th>Category</th>
              <th>Status</th>
              <th>First Ingested</th>
              <th>Last Telemetry Seen</th>
            </tr>
          </thead>
          <tbody>
            {services.map((svc) => (
              <tr key={svc.id}>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    {getIcon(svc.serviceType)}
                    <span style={{ fontWeight: 600, fontFamily: 'var(--font-mono)' }}>{svc.serviceName}</span>
                  </div>
                </td>
                <td>
                  <span className="badge badge-info">{svc.serviceType}</span>
                </td>
                <td>
                  <span className={`badge ${svc.status === 'HEALTHY' ? 'badge-success' : 'badge-critical'}`}>
                    {svc.status}
                  </span>
                </td>
                <td style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>
                  {svc.firstSeen ? new Date(svc.firstSeen).toLocaleTimeString() : '--'}
                </td>
                <td style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>
                  {svc.lastSeen ? new Date(svc.lastSeen).toLocaleTimeString() : '--'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
