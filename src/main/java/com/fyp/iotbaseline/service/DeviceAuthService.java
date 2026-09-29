package com.fyp.iotbaseline.service;

import com.fyp.iotbaseline.model.DeviceRegistration;
import com.fyp.iotbaseline.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Device Authentication Service — Conventional Baseline.
 *
 * <p>Implements basic device identification and authentication as described
 * in Chapter 3 of the project report. Uses SHA-256 hashing of a shared
 * secret key for device verification.</p>
 *
 * <p><b>SECURITY LIMITATION (Chapter 3):</b>
 * This is simple password/shared-key authentication. It lacks:
 * <ul>
 *   <li>Certificate-based mutual authentication</li>
 *   <li>Key derivation functions (e.g., PBKDF2, Argon2)</li>
 *   <li>Challenge-response protocols</li>
 *   <li>Replay attack prevention (no nonce/timestamp validation)</li>
 * </ul>
 * These are addressed in the PROPOSED SYSTEM.</p>
 */
@Service
public class DeviceAuthService {

    private static final Logger log = LoggerFactory.getLogger(DeviceAuthService.class);

    private final DeviceRepository deviceRepository;

    @Value("${simulator.default.device.id}")
    private String defaultDeviceId;

    @Value("${simulator.default.device.name}")
    private String defaultDeviceName;

    @Value("${simulator.default.secret.key}")
    private String defaultSecretKey;

    public DeviceAuthService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    /**
     * Registers a new device with hashed credentials.
     *
     * @param deviceId   unique device identifier
     * @param deviceName human-readable device name
     * @param secretKey  plaintext secret key (will be SHA-256 hashed)
     * @param deviceType device type (e.g., "ESP32")
     * @param sensorType sensor type (e.g., "DHT22")
     * @return the registered DeviceRegistration
     */
    @Transactional
    public DeviceRegistration register(String deviceId, String deviceName,
                                       String secretKey, String deviceType, String sensorType) {
        if (deviceRepository.existsByDeviceId(deviceId)) {
            throw new IllegalArgumentException("Device already registered: " + deviceId);
        }

        DeviceRegistration device = DeviceRegistration.builder()
                .deviceId(deviceId)
                .deviceName(deviceName)
                .hashedSecretKey(hashKey(secretKey))
                .deviceType(deviceType != null ? deviceType : "ESP32")
                .sensorType(sensorType != null ? sensorType : "DHT22")
                .registeredAt(LocalDateTime.now())
                .status("REGISTERED")
                .build();

        DeviceRegistration saved = deviceRepository.save(device);
        log.info("Device registered: {} ({})", deviceId, deviceName);
        return saved;
    }

    /**
     * Authenticates a device by comparing hashed keys.
     *
     * @param deviceId  the device identifier
     * @param secretKey the plaintext secret key to verify
     * @return true if credentials match
     */
    public boolean authenticate(String deviceId, String secretKey) {
        Optional<DeviceRegistration> device = deviceRepository.findByDeviceId(deviceId);
        if (device.isEmpty()) {
            log.warn("AUTH REJECTED — Unknown device: {}", deviceId);
            return false;
        }
        String hashed = hashKey(secretKey);
        boolean valid = device.get().getHashedSecretKey().equals(hashed);
        if (!valid) {
            log.warn("AUTH REJECTED — Invalid credentials for device: {}", deviceId);
        }
        return valid;
    }

    /**
     * Updates the last-seen timestamp and increments reading count.
     */
    @Transactional
    public void updateLastSeen(String deviceId) {
        deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
            device.setLastSeen(LocalDateTime.now());
            device.setStatus("ACTIVE");
            device.setTotalReadings(device.getTotalReadings() + 1);
            deviceRepository.save(device);
        });
    }

    /**
     * Increments the rejected-message counter for a device.
     */
    @Transactional
    public void incrementRejected(String deviceId) {
        deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
            device.setRejectedMessages(device.getRejectedMessages() + 1);
            deviceRepository.save(device);
        });
    }

    public Optional<DeviceRegistration> findById(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId);
    }

    public List<DeviceRegistration> findAll() {
        return deviceRepository.findAll();
    }

    /**
     * Hashes a key using SHA-256.
     * NOTE: A production system should use Argon2id or PBKDF2 with salt.
     */
    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Registers the default demo device on application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void registerDefaultDevice() {
        if (!deviceRepository.existsByDeviceId(defaultDeviceId)) {
            DeviceRegistration demo = DeviceRegistration.builder()
                    .deviceId(defaultDeviceId)
                    .deviceName(defaultDeviceName)
                    .hashedSecretKey(hashKey(defaultSecretKey))
                    .deviceType("ESP32")
                    .sensorType("DHT22")
                    .registeredAt(LocalDateTime.now())
                    .status("REGISTERED")
                    .notes("Auto-registered demo device for Review 2")
                    .build();
            deviceRepository.save(demo);
            log.info("Default demo device registered: {} | Secret: {}", defaultDeviceId, defaultSecretKey);
        }
    }
}
