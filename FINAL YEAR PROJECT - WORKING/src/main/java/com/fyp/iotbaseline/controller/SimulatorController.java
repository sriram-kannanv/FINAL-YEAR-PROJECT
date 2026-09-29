package com.fyp.iotbaseline.controller;

import com.fyp.iotbaseline.simulator.SensorSimulator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for the sensor simulator controls.
 * Used by the dashboard UI to start/stop/configure the simulator.
 */
@RestController
@RequestMapping("/api/simulator")
@RequiredArgsConstructor
public class SimulatorController {

    private final SensorSimulator sensorSimulator;

    /** GET /api/simulator/status */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(sensorSimulator.getStatus());
    }

    /** POST /api/simulator/start */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(
            @RequestBody(required = false) Map<String, Object> body) {

        if (body != null && body.containsKey("intervalSeconds")) {
            int interval = Integer.parseInt(body.get("intervalSeconds").toString());
            sensorSimulator.setInterval(interval);
        }
        sensorSimulator.start();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Simulator started",
                "status", sensorSimulator.getStatus()
        ));
    }

    /** POST /api/simulator/stop */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop() {
        sensorSimulator.stop();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Simulator stopped",
                "status", sensorSimulator.getStatus()
        ));
    }

    /** POST /api/simulator/interval */
    @PostMapping("/interval")
    public ResponseEntity<Map<String, Object>> setInterval(@RequestBody Map<String, Integer> body) {
        Integer seconds = body.get("intervalSeconds");
        if (seconds == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "intervalSeconds required"));
        }
        try {
            sensorSimulator.setInterval(seconds);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "intervalSeconds", seconds,
                    "message", "Interval updated to " + seconds + "s"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
