package io.blackbox.backend.controller;

import io.blackbox.backend.engine.AnomalyDetectionEngine;
import io.blackbox.backend.model.AnomalyEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
public class AnomalyController {

    private final AnomalyDetectionEngine anomalyDetectionEngine;

    public AnomalyController(AnomalyDetectionEngine anomalyDetectionEngine) {
        this.anomalyDetectionEngine = anomalyDetectionEngine;
    }

    @GetMapping
    public ResponseEntity<List<AnomalyEntity>> getAnomalies() {
        return ResponseEntity.ok(anomalyDetectionEngine.getAllAnomalies());
    }
}
