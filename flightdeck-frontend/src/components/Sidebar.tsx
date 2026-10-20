import React from 'react';
import { 
  Activity, 
  Network, 
  GitFork, 
  AlertTriangle, 
  PlayCircle, 
  Layers, 
  Radio,
  Sliders,
  CheckCircle2
} from 'lucide-react';

interface SidebarProps {
  currentTab: string;
  onSelectTab: (tab: string) => void;
  activeIncidentsCount: number;
}

export const Sidebar: React.FC<SidebarProps> = ({ currentTab, onSelectTab, activeIncidentsCount }) => {
  const navItems = [
    { id: 'overview', label: 'System Overview', icon: Activity },
    { id: 'dependencies', label: 'Dependency Graph', icon: Network },
    { id: 'traces', label: 'Trace Explorer', icon: GitFork },
    { id: 'incidents', label: 'Incidents & RCA', icon: AlertTriangle, badge: activeIncidentsCount > 0 ? activeIncidentsCount : undefined },
    { id: 'replay', label: 'Incident Replay', icon: PlayCircle },
    { id: 'services', label: 'Service Catalogue', icon: Layers },
    { id: 'demo', label: 'Failure Simulator & Demo', icon: Sliders },
  ];

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <div className="brand-badge">FD</div>
        <div>
          <h2 style={{ fontSize: '1.1rem', fontWeight: 800, letterSpacing: '0.05em' }}>FLIGHTDECK</h2>
          <p style={{ fontSize: '0.72rem', color: 'var(--text-dim)', fontWeight: 600 }}>RCA & RECONSTRUCTION</p>
        </div>
      </div>

      <nav className="nav-links">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentTab === item.id;
          return (
            <div
              key={item.id}
              className={`nav-item ${isActive ? 'active' : ''}`}
              onClick={() => onSelectTab(item.id)}
            >
              <Icon size={18} />
              <span style={{ flexGrow: 1 }}>{item.label}</span>
              {item.badge !== undefined && (
                <span className="badge badge-critical" style={{ padding: '2px 7px', fontSize: '0.7rem' }}>
                  {item.badge}
                </span>
              )}
            </div>
          );
        })}
      </nav>

      <div style={{ padding: '16px 20px', borderTop: '1px solid var(--border-dim)', fontSize: '0.8rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-muted)' }}>
          <span className="status-dot healthy" />
          <span style={{ fontWeight: 500 }}>Telemetry Engine Online</span>
        </div>
        <div style={{ marginTop: '4px', fontSize: '0.72rem', color: 'var(--text-dim)' }}>
          OTel Collector &bull; Kafka &bull; Redis
        </div>
      </div>
    </aside>
  );
};
