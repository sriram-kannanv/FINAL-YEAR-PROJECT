package com.fyp.iotbaseline.controller;

import com.fyp.iotbaseline.service.MonitoringService;
import com.fyp.iotbaseline.service.SensorDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for system monitoring status.
 */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final MonitoringService monitoringService;
    private final SensorDataService sensorDataService;

    public MonitoringController(MonitoringService monitoringService, SensorDataService sensorDataService) {
        this.monitoringService = monitoringService;
        this.sensorDataService = sensorDataService;
    }

    /**
     * Get full system status for dashboard.
     * GET /api/monitoring/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(monitoringService.getDashboardStatus());
    }

    /**
     * Get combined stats for the dashboard KPI cards.
     * GET /api/monitoring/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> status = monitoringService.getDashboardStatus();
        return ResponseEntity.ok(Map.of(
                "totalReadings", sensorDataService.getTotalCount(),
                "rejectedReadings", sensorDataService.getRejectedCount(),
                "totalMessages", monitoringService.getTotalMessagesReceived(),
                "rejectedMessages", monitoringService.getTotalMessagesRejected(),
                "activeDevices", status.get("activeDevices"),
                "totalDevices", status.get("totalDevices"),
                "mqttConnected", status.get("mqttConnected"),
                "tlsEnabled", false,
                "brokerRunning", status.get("brokerRunning"),
                "databaseStatus", status.get("databaseStatus")
        ));
    }
}
