package com.fyp.iotbaseline.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

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
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Builder.Default
    private Boolean simulated = false;

    /** Quality indicator: VALID, INVALID_RANGE, AUTH_FAILED */
    @Column(name = "quality")
    @Builder.Default
    private String quality = "VALID";

    @PrePersist
    public void prePersist() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }
}
