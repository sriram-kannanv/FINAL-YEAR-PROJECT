package com.fyp.iotbaseline.controller;

import com.fyp.iotbaseline.model.DeviceRegistration;
import com.fyp.iotbaseline.service.DeviceAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for device registration and status.
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private static final Logger log = LoggerFactory.getLogger(DeviceController.class);

    private final DeviceAuthService deviceAuthService;

    public DeviceController(DeviceAuthService deviceAuthService) {
        this.deviceAuthService = deviceAuthService;
    }

    /**
     * Register a new device.
     * POST /api/devices/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String deviceId  = request.get("deviceId");
        String name      = request.get("deviceName");
        String secretKey = request.get("secretKey");
        String type      = request.getOrDefault("deviceType", "ESP32");
        String sensor    = request.getOrDefault("sensorType", "DHT22");

        if (deviceId == null || name == null || secretKey == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "deviceId, deviceName, and secretKey are required"));
        }

        try {
            DeviceRegistration reg = deviceAuthService.register(deviceId, name, secretKey, type, sensor);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Device registered successfully",
                    "deviceId", reg.getDeviceId(),
                    "registeredAt", reg.getRegisteredAt().toString()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all registered devices.
     * GET /api/devices
     */
    @GetMapping
    public ResponseEntity<List<DeviceRegistration>> getAllDevices() {
        return ResponseEntity.ok(deviceAuthService.findAll());
    }

    /**
     * Get a specific device's status.
     * GET /api/devices/{deviceId}/status
     */
    @GetMapping("/{deviceId}/status")
    public ResponseEntity<?> getDeviceStatus(@PathVariable String deviceId) {
        return deviceAuthService.findById(deviceId)
                .map(d -> ResponseEntity.ok((Object) Map.of(
                        "deviceId", d.getDeviceId(),
                        "deviceName", d.getDeviceName(),
                        "status", d.getStatus(),
                        "lastSeen", d.getLastSeen() != null ? d.getLastSeen().toString() : "Never",
                        "totalReadings", d.getTotalReadings(),
                        "rejectedMessages", d.getRejectedMessages(),
                        "registeredAt", d.getRegisteredAt().toString()
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Test authentication (for demo/debugging).
     * POST /api/devices/authenticate
     */
    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody Map<String, String> request) {
        String deviceId  = request.get("deviceId");
        String secretKey = request.get("secretKey");
        if (deviceId == null || secretKey == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "deviceId and secretKey required"));
        }
        boolean valid = deviceAuthService.authenticate(deviceId, secretKey);
        return ResponseEntity.ok(Map.of(
                "authenticated", valid,
                "deviceId", deviceId,
                "message", valid ? "Authentication successful" : "Authentication failed — invalid credentials"
        ));
    }
}
