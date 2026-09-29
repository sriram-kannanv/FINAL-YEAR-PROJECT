package com.fyp.iotbaseline.service;

import com.fyp.iotbaseline.model.DeviceRegistration;
import com.fyp.iotbaseline.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Conventional Device and Communication Monitoring Service.
 *
 * <p>Provides basic availability monitoring: detects whether devices
 * are active based on their last-seen timestamp. This represents
 * the conventional baseline monitoring described in Chapter 3.</p>
 *
 * <p><b>LIMITATION (Chapter 3):</b>
 * This is basic availability monitoring only. The proposed system adds:
 * <ul>
 *   <li>Anomaly detection on sensor value patterns</li>
 *   <li>Suspicious activity detection</li>
 *   <li>Adaptive security policy responses</li>
 *   <li>Incident management and alerting</li>
 * </ul></p>
 */
@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final DeviceRepository deviceRepository;

    @Value("${monitoring.device.inactive.threshold.seconds:60}")
    private int inactiveThresholdSeconds;

    // In-memory counters
    private final AtomicBoolean mqttConnected = new AtomicBoolean(false);
    private final AtomicBoolean brokerRunning = new AtomicBoolean(false);
    private final AtomicLong totalMessagesReceived = new AtomicLong(0);
    private final AtomicLong totalMessagesRejected = new AtomicLong(0);
    private final Map<String, String> deviceStatusCache = new ConcurrentHashMap<>();

    public MonitoringService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    // -----------------------------------------------------------------------
    // MQTT and Broker status
    // -----------------------------------------------------------------------

    public void setMqttConnected(boolean connected) {
        mqttConnected.set(connected);
    }

    public void setBrokerRunning(boolean running) {
        brokerRunning.set(running);
    }

    public boolean isMqttConnected() {
        return mqttConnected.get();
    }

    public boolean isBrokerRunning() {
        return brokerRunning.get();
    }

    public void incrementReceived() {
        totalMessagesReceived.incrementAndGet();
    }

    public void incrementRejected() {
        totalMessagesRejected.incrementAndGet();
    }

    // -----------------------------------------------------------------------
    // Device activity monitoring (runs every 30 seconds)
    // -----------------------------------------------------------------------

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void checkDeviceActivity() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusSeconds(inactiveThresholdSeconds);
            List<DeviceRegistration> allDevices = deviceRepository.findAll();

            for (DeviceRegistration device : allDevices) {
                if (device.getLastSeen() == null) {
                    updateDeviceStatus(device, "NEVER_CONNECTED");
                } else if (device.getLastSeen().isBefore(threshold)) {
                    updateDeviceStatus(device, "INACTIVE");
                } else {
                    updateDeviceStatus(device, "ACTIVE");
                }
            }
        } catch (Exception e) {
            log.debug("Device activity check deferred: {}", e.getMessage());
        }
    }

    @Transactional
    private void updateDeviceStatus(DeviceRegistration device, String newStatus) {
        if (!newStatus.equals(device.getStatus())) {
            log.info("Device {} status changed: {} → {}", device.getDeviceId(), device.getStatus(), newStatus);
            device.setStatus(newStatus);
            deviceRepository.save(device);
        }
        deviceStatusCache.put(device.getDeviceId(), newStatus);
    }

    // -----------------------------------------------------------------------
    // Dashboard summary
    // -----------------------------------------------------------------------

    public Map<String, Object> getDashboardStatus() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(inactiveThresholdSeconds);
        long activeDevices = deviceRepository.countActiveDevicesSince(threshold);
        long totalDevices = deviceRepository.count();

        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("mqttConnected", mqttConnected.get());
        map.put("mqttBroker", "tcp://localhost:1883 [UNENCRYPTED - localhost only]");
        map.put("tlsEnabled", false);
        map.put("tlsNote", "TLS not active for this local demonstration. See README for TLS configuration.");
        map.put("brokerRunning", brokerRunning.get());
        map.put("brokerType", "Moquette Embedded Broker (Mosquitto replacement for local demo)");
        map.put("databaseStatus", "ONLINE");
        map.put("databaseType", "SQLite (represents cloud storage layer in local demo)");
        map.put("totalMessagesReceived", totalMessagesReceived.get());
        map.put("totalMessagesRejected", totalMessagesRejected.get());
        map.put("activeDevices", activeDevices);
        map.put("totalDevices", totalDevices);
        map.put("systemTime", LocalDateTime.now().toString());
        return map;
    }

    public long getTotalMessagesReceived() {
        return totalMessagesReceived.get();
    }

    public long getTotalMessagesRejected() {
        return totalMessagesRejected.get();
    }
}
