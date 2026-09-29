package com.fyp.iotbaseline.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fyp.iotbaseline.model.SensorReading;
import com.fyp.iotbaseline.service.DeviceAuthService;
import com.fyp.iotbaseline.service.MonitoringService;
import com.fyp.iotbaseline.service.SensorDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * MQTT Message Handler — Conventional IoT-Cloud Backend Reception.
 *
 * <p>Receives MQTT messages from the sensor simulator (or real ESP32),
 * performs basic device authentication, validates the payload, and
 * persists valid readings to SQLite.</p>
 *
 * <p>This implements steps 4–6 of the conventional workflow from Chapter 3:
 * <ol>
 *   <li>Basic device identification and authentication</li>
 *   <li>Network transmission reception</li>
 *   <li>Backend reception and processing</li>
 * </ol></p>
 *
 * <p><b>SECURITY LIMITATION:</b>
 * The payload arrives in plaintext JSON. There is no source-level encryption.
 * Authentication is via a shared secret key in the payload body — which is
 * visible in transit (since TLS is not active for this demo).
 * The proposed system addresses this with AES-256-GCM and TLS together.</p>
 */
@Component
public class MqttMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageHandler.class);

    private final DeviceAuthService deviceAuthService;
    private final SensorDataService sensorDataService;
    private final MonitoringService monitoringService;
    private final ObjectMapper objectMapper;

    // Sensor value validity ranges
    private static final double TEMP_MIN = -40.0;
    private static final double TEMP_MAX = 80.0;
    private static final double HUMIDITY_MIN = 0.0;
    private static final double HUMIDITY_MAX = 100.0;

    public MqttMessageHandler(DeviceAuthService deviceAuthService,
                              SensorDataService sensorDataService,
                              MonitoringService monitoringService) {
        this.deviceAuthService = deviceAuthService;
        this.sensorDataService = sensorDataService;
        this.monitoringService = monitoringService;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /**
     * Handles an incoming MQTT message.
     *
     * @param topic   the MQTT topic (e.g., {@code iot/sensor/ESP32-DEMO-001/data})
     * @param payload the raw JSON string payload
     */
    public void handleMessage(String topic, String payload) {
        monitoringService.incrementReceived();
        log.debug("MQTT message received on topic: {}", topic);

        try {
            JsonNode node = objectMapper.readTree(payload);

            // 1. Extract required fields
            String deviceId = getRequired(node, "deviceId");
            String secretKey = getRequired(node, "secretKey");
            double temperature = getRequiredDouble(node, "temperature");
            double humidity = getRequiredDouble(node, "humidity");
            String timestampStr = node.has("timestamp") ? node.get("timestamp").asText() : null;
            boolean simulated = node.has("simulated") && node.get("simulated").asBoolean();

            // 2. Basic device authentication (conventional baseline)
            if (!deviceAuthService.authenticate(deviceId, secretKey)) {
                log.warn("REJECTED — Auth failed for device: {} on topic: {}", deviceId, topic);
                monitoringService.incrementRejected();
                deviceAuthService.incrementRejected(deviceId);
                saveFailed(deviceId, topic, payload, "AUTH_FAILED");
                return;
            }

            // 3. Validate sensor value ranges
            if (!isValidRange(temperature, TEMP_MIN, TEMP_MAX)) {
                log.warn("REJECTED — Temperature out of range: {} for device: {}", temperature, deviceId);
                monitoringService.incrementRejected();
                saveFailed(deviceId, topic, payload, "INVALID_RANGE");
                return;
            }
            if (!isValidRange(humidity, HUMIDITY_MIN, HUMIDITY_MAX)) {
                log.warn("REJECTED — Humidity out of range: {} for device: {}", humidity, deviceId);
                monitoringService.incrementRejected();
                saveFailed(deviceId, topic, payload, "INVALID_RANGE");
                return;
            }

            // 4. Parse timestamp
            LocalDateTime timestamp;
            try {
                timestamp = (timestampStr != null)
                        ? LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : LocalDateTime.now();
            } catch (DateTimeParseException e) {
                timestamp = LocalDateTime.now();
            }

            // 5. Get device name
            String deviceName = node.has("deviceName") ? node.get("deviceName").asText() : deviceId;

            // 6. Persist reading
            SensorReading reading = SensorReading.builder()
                    .deviceId(deviceId)
                    .deviceName(deviceName)
                    .temperature(temperature)
                    .humidity(humidity)
                    .timestamp(timestamp)
                    .receivedAt(LocalDateTime.now())
                    .mqttTopic(topic)
                    .rawPayload(payload)
                    .simulated(simulated)
                    .quality("VALID")
                    .build();

            sensorDataService.save(reading);
            deviceAuthService.updateLastSeen(deviceId);

            log.info("ACCEPTED — device={} temp={}°C humidity={}% simulated={}",
                    deviceId, temperature, humidity, simulated);

        } catch (Exception e) {
            log.error("Error processing MQTT message on topic {}: {}", topic, e.getMessage());
            monitoringService.incrementRejected();
        }
    }

    private String getRequired(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return node.get(field).asText();
    }

    private double getRequiredDouble(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return node.get(field).asDouble();
    }

    private boolean isValidRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    private void saveFailed(String deviceId, String topic, String payload, String quality) {
        try {
            SensorReading failed = SensorReading.builder()
                    .deviceId(deviceId)
                    .temperature(0.0)
                    .humidity(0.0)
                    .timestamp(LocalDateTime.now())
                    .mqttTopic(topic)
                    .rawPayload(payload)
                    .simulated(false)
                    .quality(quality)
                    .build();
            sensorDataService.save(failed);
        } catch (Exception e) {
            log.error("Could not save failed reading record: {}", e.getMessage());
        }
    }
}
