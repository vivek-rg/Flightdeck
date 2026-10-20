import React, { useState, useEffect } from 'react';
import { Sidebar } from './components/Sidebar';
import { OverviewView } from './components/OverviewView';
import { DependencyGraphView } from './components/DependencyGraphView';
import { TraceExplorerView } from './components/TraceExplorerView';
import { IncidentsView } from './components/IncidentsView';
import { IncidentDetailView } from './components/IncidentDetailView';
import { ReplayView } from './components/ReplayView';
import { ServicesView } from './components/ServicesView';
import { DemoScenarioView } from './components/DemoScenarioView';
import { 
  fetchDashboardSummary, 
  fetchServices, 
  fetchDependencies, 
  fetchTraces, 
  fetchIncidents 
} from './api';

export const App: React.FC = () => {
  const [currentTab, setCurrentTab] = useState<string>('overview');
  const [selectedIncidentId, setSelectedIncidentId] = useState<string | null>(null);
  const [selectedReplayId, setSelectedReplayId] = useState<string | undefined>(undefined);

  const [summary, setSummary] = useState<any>(null);
  const [services, setServices] = useState<any[]>([]);
  const [graphData, setGraphData] = useState<any>({ nodes: [], edges: [] });
  const [traces, setTraces] = useState<any[]>([]);
  const [incidents, setIncidents] = useState<any[]>([]);

  const loadAllData = async () => {
    try {
      const [sum, svcs, deps, trs, incs] = await Promise.all([
        fetchDashboardSummary().catch(() => null),
        fetchServices().catch(() => []),
        fetchDependencies().catch(() => ({ nodes: [], edges: [] })),
        fetchTraces().catch(() => []),
        fetchIncidents().catch(() => [])
      ]);

      if (sum) setSummary(sum);
      setServices(svcs);
      setGraphData(deps);
      setTraces(trs);
      setIncidents(incs);
    } catch (err) {
      console.error('Data reload error:', err);
    }
  };

  useEffect(() => {
    loadAllData();
    const interval = setInterval(loadAllData, 10000); // 10s auto-refresh
    return () => clearInterval(interval);
  }, []);

  const handleSelectIncident = (id: string) => {
    setSelectedIncidentId(id);
    setCurrentTab('incident-detail');
  };

  const handleNavigateToReplay = (replayId?: string) => {
    setSelectedReplayId(replayId);
    setCurrentTab('replay');
  };

  return (
    <div className="app-container">
      <Sidebar
        currentTab={currentTab}
        onSelectTab={(tab) => {
          setCurrentTab(tab);
          if (tab !== 'incident-detail') {
            setSelectedIncidentId(null);
          }
        }}
        activeIncidentsCount={summary?.activeIncidents || 0}
      />

      <main className="content-area">
        {currentTab === 'overview' && (
          <OverviewView
            summary={summary}
            onSelectIncident={handleSelectIncident}
            onNavigate={(tab) => setCurrentTab(tab)}
          />
        )}

        {currentTab === 'dependencies' && (
          <DependencyGraphView
            graphData={graphData}
            onRefresh={loadAllData}
          />
        )}

        {currentTab === 'traces' && (
          <TraceExplorerView
            traces={traces}
          />
        )}

        {currentTab === 'incidents' && (
          <IncidentsView
            incidents={incidents}
            onSelectIncident={handleSelectIncident}
          />
        )}

        {currentTab === 'incident-detail' && selectedIncidentId && (
          <IncidentDetailView
            incidentId={selectedIncidentId}
            onBack={() => setCurrentTab('incidents')}
            onNavigateToReplay={handleNavigateToReplay}
          />
        )}

        {currentTab === 'replay' && (
          <ReplayView
            initialReplayId={selectedReplayId}
            onSelectIncident={handleSelectIncident}
          />
        )}

        {currentTab === 'services' && (
          <ServicesView
            services={services}
            onRefresh={loadAllData}
          />
        )}

        {currentTab === 'demo' && (
          <DemoScenarioView
            onSelectIncident={handleSelectIncident}
            onNavigateToReplay={handleNavigateToReplay}
            onRefreshAll={loadAllData}
          />
        )}
      </main>
    </div>
  );
};
