package io.blackbox.backend.controller;

import io.blackbox.backend.engine.DependencyDiscoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dependencies")
public class DependencyController {

    private final DependencyDiscoveryService dependencyDiscoveryService;

    public DependencyController(DependencyDiscoveryService dependencyDiscoveryService) {
        this.dependencyDiscoveryService = dependencyDiscoveryService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDependencies() {
        return ResponseEntity.ok(dependencyDiscoveryService.getGraphData());
    }
}
