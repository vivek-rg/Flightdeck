package io.blackbox.backend.controller;

import io.blackbox.backend.dto.TraceDetailDto;
import io.blackbox.backend.engine.TraceReconstructionEngine;
import io.blackbox.backend.model.TraceEntity;
import io.blackbox.backend.repository.TraceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traces")
public class TraceController {

    private final TraceReconstructionEngine traceEngine;
    private final TraceRepository traceRepository;

    public TraceController(TraceReconstructionEngine traceEngine, TraceRepository traceRepository) {
        this.traceEngine = traceEngine;
        this.traceRepository = traceRepository;
    }

    @GetMapping
    public ResponseEntity<List<TraceEntity>> getRecentTraces() {
        return ResponseEntity.ok(traceRepository.findRecentTraces());
    }

    @GetMapping("/{traceId}")
    public ResponseEntity<TraceDetailDto> getTrace(@PathVariable String traceId) {
        return ResponseEntity.ok(traceEngine.reconstructTrace(traceId));
    }
}
