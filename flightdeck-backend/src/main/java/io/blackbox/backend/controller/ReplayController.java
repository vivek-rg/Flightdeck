package io.blackbox.backend.controller;

import io.blackbox.backend.engine.IncidentReplayEngine;
import io.blackbox.backend.model.ReplayRunEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/replay")
public class ReplayController {

    private final IncidentReplayEngine replayEngine;

    public ReplayController(IncidentReplayEngine replayEngine) {
        this.replayEngine = replayEngine;
    }

    @PostMapping("/{incidentId}")
    public ResponseEntity<ReplayRunEntity> triggerReplay(@PathVariable String incidentId) {
        return ResponseEntity.ok(replayEngine.executeReplay(incidentId));
    }

    @GetMapping("/{replayId}")
    public ResponseEntity<ReplayRunEntity> getReplayRun(@PathVariable String replayId) {
        return ResponseEntity.ok(replayEngine.getReplay(replayId));
    }

    @GetMapping("/incident/{incidentId}")
    public ResponseEntity<List<ReplayRunEntity>> getReplaysForIncident(@PathVariable String incidentId) {
        return ResponseEntity.ok(replayEngine.getReplaysForIncident(incidentId));
    }
}
