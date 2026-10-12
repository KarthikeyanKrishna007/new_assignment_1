package com.flock.urjawrapper.controller;

import com.flock.urjawrapper.dto.MeterResponse;
import com.flock.urjawrapper.dto.ConsumptionResponse;
import com.flock.urjawrapper.dto.NetworkNodeResponse;
import com.flock.urjawrapper.service.UrjaPortalClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Urja Modern Smart Meter Operations Core Engine")
public class MeterController {

    private final UrjaPortalClient client;

    public MeterController(UrjaPortalClient client) {
        this.client = client;
    }

    @GetMapping("/meters")
    @Operation(summary = "Query unified smart meter fleet catalogue")
    public ResponseEntity<List<MeterResponse>> getAllMeters() {
        return ResponseEntity.ok(client.scrapeAllMeters());
    }

    @GetMapping("/meters/{meterId}/consumption")
    @Operation(summary = "Stream normalized telemetry time-series logs sequence")
    public ResponseEntity<List<ConsumptionResponse>> getMeterConsumption(@PathVariable String meterId) {
        List<ConsumptionResponse> timeline = client.scrapeConsumptionHistory(meterId);
        if (timeline.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(timeline);
    }

    @GetMapping("/network/hierarchy")
    @Operation(summary = "Build grid structural nodes topology map")
    public ResponseEntity<List<NetworkNodeResponse>> getGridTopologyTree() {
        return ResponseEntity.ok(client.reconstructGridHierarchy());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> catchSystemAnomalies(RuntimeException ex) {
        Map<String, String> alertMap = new HashMap<>();
        alertMap.put("error", "Bad Gateway proxy handshake breakdown");
        alertMap.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(alertMap);
    }
}
