package io.blackbox.backend.controller;

import io.blackbox.backend.engine.IncidentDetectionEngine;
import io.blackbox.backend.engine.LlmExplanationService;
import io.blackbox.backend.engine.RootCauseAnalysisEngine;
import io.blackbox.backend.model.IncidentEntity;
import io.blackbox.backend.model.IncidentEventEntity;
import io.blackbox.backend.model.RootCauseCandidateEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentDetectionEngine incidentDetectionEngine;
    private final RootCauseAnalysisEngine rootCauseAnalysisEngine;
    private final LlmExplanationService llmExplanationService;

    public IncidentController(IncidentDetectionEngine incidentDetectionEngine,
                              RootCauseAnalysisEngine rootCauseAnalysisEngine,
                              LlmExplanationService llmExplanationService) {
        this.incidentDetectionEngine = incidentDetectionEngine;
        this.rootCauseAnalysisEngine = rootCauseAnalysisEngine;
        this.llmExplanationService = llmExplanationService;
    }

    @GetMapping
    public ResponseEntity<List<IncidentEntity>> getIncidents() {
        return ResponseEntity.ok(incidentDetectionEngine.getAllIncidents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentEntity> getIncident(@PathVariable String id) {
        return ResponseEntity.ok(incidentDetectionEngine.getIncident(id));
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<IncidentEventEntity>> getIncidentTimeline(@PathVariable String id) {
        return ResponseEntity.ok(incidentDetectionEngine.getTimeline(id));
    }

    @GetMapping("/{id}/root-cause")
    public ResponseEntity<List<RootCauseCandidateEntity>> getRootCauseCandidates(@PathVariable String id) {
        return ResponseEntity.ok(rootCauseAnalysisEngine.getCandidatesForIncident(id));
    }

    @PostMapping("/{id}/explain")
    public ResponseEntity<Map<String, Object>> getLlmExplanation(@PathVariable String id) {
        return ResponseEntity.ok(llmExplanationService.generateExplanation(id));
    }
}
