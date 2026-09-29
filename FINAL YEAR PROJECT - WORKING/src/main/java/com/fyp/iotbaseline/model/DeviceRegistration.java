package com.fyp.iotbaseline.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a registered IoT device (e.g., ESP32 node).
 * Conventional baseline: simple password/shared-key registration.
 */
@Entity
@Table(name = "device_registrations")
public class DeviceRegistration {

    @Id
    @Column(name = "device_id", nullable = false, unique = true)
    private String deviceId;

    @Column(name = "device_name", nullable = false)
    private String deviceName;

    @Column(name = "hashed_secret_key", nullable = false)
    private String hashedSecretKey;

    @Column(name = "device_type")
    private String deviceType = "ESP32";

    @Column(name = "sensor_type")
    private String sensorType = "DHT22";

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "status")
    private String status = "REGISTERED";

    @Column(name = "total_readings")
    private Long totalReadings = 0L;

    @Column(name = "rejected_messages")
    private Long rejectedMessages = 0L;

    @Column(name = "notes")
    private String notes;

    public DeviceRegistration() {}

    @PrePersist
    public void prePersist() {
        if (registeredAt == null) registeredAt = LocalDateTime.now();
    }

    // -------------------------------------------------------
    // Builder pattern (without Lombok)
    // -------------------------------------------------------
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String deviceId;
        private String deviceName;
        private String hashedSecretKey;
        private String deviceType = "ESP32";
        private String sensorType = "DHT22";
        private LocalDateTime registeredAt;
        private LocalDateTime lastSeen;
        private String status = "REGISTERED";
        private Long totalReadings = 0L;
        private Long rejectedMessages = 0L;
        private String notes;

        public Builder deviceId(String v)          { deviceId = v; return this; }
        public Builder deviceName(String v)        { deviceName = v; return this; }
        public Builder hashedSecretKey(String v)   { hashedSecretKey = v; return this; }
        public Builder deviceType(String v)        { deviceType = v; return this; }
        public Builder sensorType(String v)        { sensorType = v; return this; }
        public Builder registeredAt(LocalDateTime v){ registeredAt = v; return this; }
        public Builder lastSeen(LocalDateTime v)   { lastSeen = v; return this; }
        public Builder status(String v)            { status = v; return this; }
        public Builder totalReadings(Long v)       { totalReadings = v; return this; }
        public Builder rejectedMessages(Long v)    { rejectedMessages = v; return this; }
        public Builder notes(String v)             { notes = v; return this; }

        public DeviceRegistration build() {
            DeviceRegistration d = new DeviceRegistration();
            d.deviceId = deviceId;
            d.deviceName = deviceName;
            d.hashedSecretKey = hashedSecretKey;
            d.deviceType = deviceType;
            d.sensorType = sensorType;
            d.registeredAt = registeredAt;
            d.lastSeen = lastSeen;
            d.status = status;
            d.totalReadings = totalReadings;
            d.rejectedMessages = rejectedMessages;
            d.notes = notes;
            return d;
        }
    }

    // -------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------
    public String getDeviceId()           { return deviceId; }
    public void   setDeviceId(String v)   { deviceId = v; }

    public String getDeviceName()         { return deviceName; }
    public void   setDeviceName(String v) { deviceName = v; }

    public String getHashedSecretKey()          { return hashedSecretKey; }
    public void   setHashedSecretKey(String v)  { hashedSecretKey = v; }

    public String getDeviceType()           { return deviceType; }
    public void   setDeviceType(String v)   { deviceType = v; }

    public String getSensorType()           { return sensorType; }
    public void   setSensorType(String v)   { sensorType = v; }

    public LocalDateTime getRegisteredAt()          { return registeredAt; }
    public void          setRegisteredAt(LocalDateTime v) { registeredAt = v; }

    public LocalDateTime getLastSeen()            { return lastSeen; }
    public void          setLastSeen(LocalDateTime v) { lastSeen = v; }

    public String getStatus()           { return status; }
    public void   setStatus(String v)   { status = v; }

    public Long getTotalReadings()            { return totalReadings; }
    public void setTotalReadings(Long v)      { totalReadings = v; }

    public Long getRejectedMessages()         { return rejectedMessages; }
    public void setRejectedMessages(Long v)   { rejectedMessages = v; }

    public String getNotes()          { return notes; }
    public void   setNotes(String v)  { notes = v; }

    @Override
    public String toString() {
        return "DeviceRegistration{deviceId='" + deviceId + "', status='" + status + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceRegistration d)) return false;
        return Objects.equals(deviceId, d.deviceId);
    }

    @Override
    public int hashCode() { return Objects.hash(deviceId); }
}
