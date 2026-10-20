package io.blackbox.sample.inventory.controller;

import io.blackbox.sample.common.dto.InventoryReservationDto;
import io.blackbox.sample.common.tracing.TraceContextUtils;
import io.blackbox.sample.inventory.model.InventoryEntity;
import io.blackbox.sample.inventory.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/reserve")
    public ResponseEntity<InventoryReservationDto> reserveStock(@RequestBody InventoryReservationDto request) {
        InventoryReservationDto result = inventoryService.reserveStock(request);
        if ("FAILED".equalsIgnoreCase(result.getStatus())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(TraceContextUtils.X_TRACE_ID_HEADER, TraceContextUtils.getCurrentTraceId())
                    .body(result);
        }
        return ResponseEntity.ok()
                .header(TraceContextUtils.X_TRACE_ID_HEADER, TraceContextUtils.getCurrentTraceId())
                .body(result);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryEntity> getStock(@PathVariable(name = "productId") String productId) {
        return ResponseEntity.ok(inventoryService.getStock(productId));
    }
}
