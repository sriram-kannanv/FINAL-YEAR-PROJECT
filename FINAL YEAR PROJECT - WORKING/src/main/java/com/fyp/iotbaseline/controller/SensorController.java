package com.fyp.iotbaseline.controller;

import com.fyp.iotbaseline.model.SensorReading;
import com.fyp.iotbaseline.service.SensorDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API for sensor readings.
 */
@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
public class SensorController {

    private final SensorDataService sensorDataService;

    /**
     * Get all readings (paged).
     * GET /api/sensors/readings?page=0&size=20
     */
    @GetMapping("/readings")
    public ResponseEntity<Map<String, Object>> getReadings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String deviceId) {

        Page<SensorReading> data = (deviceId != null && !deviceId.isBlank())
                ? sensorDataService.getByDevicePaged(deviceId, page, size)
                : sensorDataService.getAllPaged(page, size);

        return ResponseEntity.ok(Map.of(
                "readings", data.getContent(),
                "totalElements", data.getTotalElements(),
                "totalPages", data.getTotalPages(),
                "currentPage", page
        ));
    }

    /**
     * Get the most recent reading for a device (or any device).
     * GET /api/sensors/readings/latest?deviceId=ESP32-DEMO-001
     */
    @GetMapping("/readings/latest")
    public ResponseEntity<?> getLatest(@RequestParam(required = false) String deviceId) {
        Optional<SensorReading> reading = (deviceId != null && !deviceId.isBlank())
                ? sensorDataService.getLatest(deviceId)
                : sensorDataService.getLatestAny();

        return reading
                .map(r -> ResponseEntity.ok((Object) r))
                .orElseGet(() -> ResponseEntity.ok(Map.of("message", "No readings yet")));
    }

    /**
     * Get reading history for a device within a time range.
     * GET /api/sensors/readings/{deviceId}/history?from=...&to=...
     */
    @GetMapping("/readings/{deviceId}/history")
    public ResponseEntity<List<SensorReading>> getHistory(
            @PathVariable String deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        if (from == null) from = LocalDateTime.now().minusHours(24);
        if (to == null)   to   = LocalDateTime.now();

        return ResponseEntity.ok(sensorDataService.getHistory(deviceId, from, to));
    }

    /**
     * Get recent valid readings for chart rendering (last N hours).
     * GET /api/sensors/readings/chart?hours=24&deviceId=...
     */
    @GetMapping("/readings/chart")
    public ResponseEntity<List<SensorReading>> getChartData(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(required = false) String deviceId) {

        if (deviceId != null && !deviceId.isBlank()) {
            LocalDateTime from = LocalDateTime.now().minusHours(hours);
            LocalDateTime to   = LocalDateTime.now();
            List<SensorReading> readings = sensorDataService.getHistory(deviceId, from, to);
            return ResponseEntity.ok(readings.stream()
                    .filter(r -> "VALID".equals(r.getQuality()))
                    .toList());
        }
        return ResponseEntity.ok(sensorDataService.getValidReadingsSince(hours));
    }

    /**
     * Get summary statistics.
     * GET /api/sensors/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
                "totalReadings", sensorDataService.getTotalCount(),
                "rejectedReadings", sensorDataService.getRejectedCount()
        ));
    }
}
