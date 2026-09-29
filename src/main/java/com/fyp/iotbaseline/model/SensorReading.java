package com.fyp.iotbaseline.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a sensor data reading received from an IoT device.
 *
 * <p>The payload is received in plaintext over MQTT.
 * NOTE: In the conventional baseline, sensor data is NOT encrypted
 * at source. This is a core limitation that the proposed system
 * addresses with AES-256-GCM source-level encryption.</p>
 */
@Entity
@Table(name = "sensor_readings",
       indexes = {
           @Index(name = "idx_device_id", columnList = "device_id"),
           @Index(name = "idx_timestamp", columnList = "timestamp")
       })
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "device_name")
    private String deviceName;

    /** Temperature in Celsius from DHT11/DHT22 sensor. */
    @Column(name = "temperature", nullable = false)
    private Double temperature;

    /** Relative humidity percentage from DHT11/DHT22 sensor. */
    @Column(name = "humidity", nullable = false)
    private Double humidity;

    /** Timestamp of the reading (set by simulator/device). */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /** When the backend received this reading. */
    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    /** MQTT topic this reading was published on. */
    @Column(name = "mqtt_topic")
    private String mqttTopic;

    /**
     * Raw JSON payload as received over MQTT.
     * NOTE: This is stored in plaintext — no source-level encryption.
     * In the proposed system, this would be an AES-256-GCM ciphertext.
     */
    @Column(name = "raw_payload", length = 2048)
    private String rawPayload;

    /** Whether this reading came from the software simulator. */
    @Column(name = "simulated")
    private Boolean simulated = false;

    /** Quality indicator: VALID, INVALID_RANGE, AUTH_FAILED */
    @Column(name = "quality")
    private String quality = "VALID";

    public SensorReading() {}

    public SensorReading(Long id, String deviceId, String deviceName, Double temperature, Double humidity,
                         LocalDateTime timestamp, LocalDateTime receivedAt, String mqttTopic,
                         String rawPayload, Boolean simulated, String quality) {
        this.id = id;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.temperature = temperature;
        this.humidity = humidity;
        this.timestamp = timestamp;
        this.receivedAt = receivedAt;
        this.mqttTopic = mqttTopic;
        this.rawPayload = rawPayload;
        this.simulated = simulated != null ? simulated : false;
        this.quality = quality != null ? quality : "VALID";
    }

    @PrePersist
    public void prePersist() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String deviceId;
        private String deviceName;
        private Double temperature;
        private Double humidity;
        private LocalDateTime timestamp;
        private LocalDateTime receivedAt;
        private String mqttTopic;
        private String rawPayload;
        private Boolean simulated = false;
        private String quality = "VALID";

        public Builder id(Long id) { this.id = id; return this; }
        public Builder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public Builder deviceName(String deviceName) { this.deviceName = deviceName; return this; }
        public Builder temperature(Double temperature) { this.temperature = temperature; return this; }
        public Builder humidity(Double humidity) { this.humidity = humidity; return this; }
        public Builder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
        public Builder receivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; return this; }
        public Builder mqttTopic(String mqttTopic) { this.mqttTopic = mqttTopic; return this; }
        public Builder rawPayload(String rawPayload) { this.rawPayload = rawPayload; return this; }
        public Builder simulated(Boolean simulated) { this.simulated = simulated; return this; }
        public Builder quality(String quality) { this.quality = quality; return this; }

        public SensorReading build() {
            return new SensorReading(id, deviceId, deviceName, temperature, humidity,
                    timestamp, receivedAt, mqttTopic, rawPayload, simulated, quality);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Double getHumidity() { return humidity; }
    public void setHumidity(Double humidity) { this.humidity = humidity; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }

    public String getMqttTopic() { return mqttTopic; }
    public void setMqttTopic(String mqttTopic) { this.mqttTopic = mqttTopic; }

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }

    public Boolean getSimulated() { return simulated; }
    public void setSimulated(Boolean simulated) { this.simulated = simulated; }

    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SensorReading reading = (SensorReading) o;
        return Objects.equals(id, reading.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SensorReading{" +
                "id=" + id +
                ", deviceId='" + deviceId + '\'' +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                ", timestamp=" + timestamp +
                '}';
    }
}
