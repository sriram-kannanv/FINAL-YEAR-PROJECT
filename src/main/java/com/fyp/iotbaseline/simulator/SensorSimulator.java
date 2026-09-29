package com.fyp.iotbaseline.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ESP32 Sensor Simulator.
 *
 * <p>Simulates a physical ESP32 microcontroller with DHT11/DHT22 sensors,
 * generating realistic temperature and humidity readings and publishing
 * them via MQTT. This replaces physical hardware for the Review 2 demo.</p>
 *
 * <p>All payloads are clearly marked with {@code "simulated": true}.
 * The data format, topic structure, and authentication method are identical
 * to what a real ESP32 firmware would produce, ensuring future hardware
 * compatibility.</p>
 *
 * <p><b>SECURITY NOTE:</b> Data is published in plaintext JSON.
 * The proposed system will add AES-256-GCM encryption at this layer.</p>
 */
@Service
public class SensorSimulator {

    private static final Logger log = LoggerFactory.getLogger(SensorSimulator.class);

    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;

    @Value("${simulator.default.device.id}")
    private String defaultDeviceId;

    @Value("${simulator.default.device.name}")
    private String defaultDeviceName;

    @Value("${simulator.default.secret.key}")
    private String defaultSecretKey;

    @Value("${simulator.default.interval.seconds:5}")
    private int defaultIntervalSeconds;

    @Value("${simulator.temperature.min:18.0}")
    private double tempMin;

    @Value("${simulator.temperature.max:42.0}")
    private double tempMax;

    @Value("${simulator.humidity.min:35.0}")
    private double humidityMin;

    @Value("${simulator.humidity.max:90.0}")
    private double humidityMax;

    @Value("${mqtt.broker.topic.publish}")
    private String baseTopic;

    // State
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger intervalSeconds = new AtomicInteger(5);
    private final AtomicLong publishedCount = new AtomicLong(0);
    private final AtomicLong failedCount = new AtomicLong(0);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> currentTask;

    // Realistic sensor drift simulation
    private final Random random = new Random();
    private double lastTemp = 25.0;
    private double lastHumidity = 60.0;

    public SensorSimulator(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public AtomicInteger getIntervalSeconds() {
        return intervalSeconds;
    }

    public AtomicLong getPublishedCount() {
        return publishedCount;
    }

    public AtomicLong getFailedCount() {
        return failedCount;
    }

    /**
     * Auto-starts the simulator on application startup.
     */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void autoStartOnStartup() {
        log.info("Auto-starting Sensor Simulator on application startup...");
        start();
    }

    /**
     * Starts the simulator for the default demo device.
     */
    public synchronized void start() {
        start(defaultDeviceId, defaultSecretKey, defaultIntervalSeconds);
    }

    /**
     * Starts the simulator with specified parameters.
     */
    public synchronized void start(String deviceId, String secretKey, int intervalSecs) {
        if (running.get()) {
            log.info("Simulator already running, restarting with new parameters...");
            stop();
        }
        intervalSeconds.set(intervalSecs);
        running.set(true);

        currentTask = scheduler.scheduleAtFixedRate(
                () -> publishReading(deviceId, secretKey),
                0, intervalSecs, TimeUnit.SECONDS);

        log.info("Sensor Simulator STARTED — device={}, interval={}s", deviceId, intervalSecs);
    }

    /**
     * Stops the simulator.
     */
    public synchronized void stop() {
        running.set(false);
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel(false);
        }
        log.info("Sensor Simulator STOPPED");
    }

    /**
     * Sets a new reporting interval and restarts if running.
     */
    public synchronized void setInterval(int seconds) {
        if (seconds < 1 || seconds > 3600) {
            throw new IllegalArgumentException("Interval must be between 1 and 3600 seconds");
        }
        intervalSeconds.set(seconds);
        if (running.get()) {
            start(defaultDeviceId, defaultSecretKey, seconds);
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * Generates and publishes a single sensor reading.
     * Simulates realistic DHT22 sensor behaviour with gradual drift.
     */
    private void publishReading(String deviceId, String secretKey) {
        try {
            // Simulate realistic sensor values with gradual drift
            double temp = simulateNextValue(lastTemp, tempMin, tempMax, 0.8);
            double humidity = simulateNextValue(lastHumidity, humidityMin, humidityMax, 1.5);
            lastTemp = temp;
            lastHumidity = humidity;

            String topic = baseTopic + "/" + deviceId + "/data";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            Map<String, Object> payload = new HashMap<>();
            payload.put("deviceId", deviceId);
            payload.put("deviceName", defaultDeviceName);
            payload.put("secretKey", secretKey);
            payload.put("temperature", Math.round(temp * 10.0) / 10.0);
            payload.put("humidity", Math.round(humidity * 10.0) / 10.0);
            payload.put("timestamp", timestamp);
            payload.put("simulated", true);
            payload.put("sensorType", "DHT22");
            payload.put("unit_temp", "Celsius");
            payload.put("unit_humidity", "Percent");
            // SECURITY NOTE: payload transmitted in plaintext (conventional baseline)
            payload.put("encryption", "NONE");

            String json = objectMapper.writeValueAsString(payload);
            MqttMessage message = new MqttMessage(json.getBytes());
            message.setQos(1);
            message.setRetained(false);

            if (mqttClient.isConnected()) {
                mqttClient.publish(topic, message);
                publishedCount.incrementAndGet();
                log.info("SIMULATOR → MQTT [{}] temp={}°C humidity={}%", topic,
                        payload.get("temperature"), payload.get("humidity"));
            } else {
                log.warn("SIMULATOR: MQTT client not connected, skipping publish");
                failedCount.incrementAndGet();
            }
        } catch (Exception e) {
            failedCount.incrementAndGet();
            log.error("Simulator publish error: {}", e.getMessage());
        }
    }

    /**
     * Simulates realistic sensor drift: gradual changes with small noise.
     */
    private double simulateNextValue(double current, double min, double max, double maxDrift) {
        double drift = (random.nextDouble() - 0.5) * 2 * maxDrift;
        double next = current + drift;
        // Clamp to range
        return Math.min(max, Math.max(min, next));
    }

    public Map<String, Object> getStatus() {
        return Map.of(
                "running", running.get(),
                "intervalSeconds", intervalSeconds.get(),
                "publishedCount", publishedCount.get(),
                "failedCount", failedCount.get(),
                "deviceId", defaultDeviceId,
                "deviceName", defaultDeviceName,
                "mqttConnected", mqttClient.isConnected(),
                "note", "Simulating ESP32 with DHT22 sensor — plaintext MQTT payload (no source-level encryption)"
        );
    }
}
