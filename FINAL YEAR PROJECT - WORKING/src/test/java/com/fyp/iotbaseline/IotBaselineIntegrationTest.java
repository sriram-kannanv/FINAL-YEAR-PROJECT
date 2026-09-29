package com.fyp.iotbaseline;

import com.fyp.iotbaseline.service.DeviceAuthService;
import com.fyp.iotbaseline.model.DeviceRegistration;
import com.fyp.iotbaseline.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the conventional IoT baseline system.
 *
 * Tests:
 * 1. Device registration
 * 2. Device authentication (valid credentials)
 * 3. Device authentication rejection (invalid credentials)
 * 4. Duplicate registration rejection
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "mqtt.embedded.broker.port=1885",
    "mqtt.broker.url=tcp://localhost:1885",
    "mqtt.broker.clientId=test-client",
    "simulator.default.device.id=TEST-DEVICE-001",
    "simulator.default.device.name=Test Device",
    "simulator.default.secret.key=test-secret"
})
class IotBaselineIntegrationTest {

    @Autowired
    private DeviceAuthService deviceAuthService;

    @Autowired
    private DeviceRepository deviceRepository;

    @BeforeEach
    void setup() {
        deviceRepository.deleteAll();
    }

    @Test
    @DisplayName("Device registration should persist device with hashed key")
    void testDeviceRegistration() {
        DeviceRegistration reg = deviceAuthService.register(
                "TEST-ESP32-001", "Test Sensor", "secret123", "ESP32", "DHT22");

        assertThat(reg).isNotNull();
        assertThat(reg.getDeviceId()).isEqualTo("TEST-ESP32-001");
        assertThat(reg.getDeviceName()).isEqualTo("Test Sensor");
        // Key should be hashed, not stored in plaintext
        assertThat(reg.getHashedSecretKey()).isNotEqualTo("secret123");
        assertThat(reg.getHashedSecretKey()).hasSize(64); // SHA-256 hex = 64 chars
    }

    @Test
    @DisplayName("Authentication should succeed with correct credentials")
    void testAuthenticationSuccess() {
        deviceAuthService.register("TEST-ESP32-002", "Node 2", "correct-key", "ESP32", "DHT22");
        boolean result = deviceAuthService.authenticate("TEST-ESP32-002", "correct-key");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Authentication should fail with wrong secret key")
    void testAuthenticationFailure_WrongKey() {
        deviceAuthService.register("TEST-ESP32-003", "Node 3", "actual-key", "ESP32", "DHT22");
        boolean result = deviceAuthService.authenticate("TEST-ESP32-003", "wrong-key");
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Authentication should fail for unknown device")
    void testAuthenticationFailure_UnknownDevice() {
        boolean result = deviceAuthService.authenticate("UNKNOWN-DEVICE", "any-key");
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Duplicate device registration should throw exception")
    void testDuplicateRegistrationRejected() {
        deviceAuthService.register("TEST-ESP32-004", "Node 4", "key1", "ESP32", "DHT22");
        assertThatThrownBy(() ->
                deviceAuthService.register("TEST-ESP32-004", "Node 4 Copy", "key2", "ESP32", "DHT22")
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("Device lookup should return registered device")
    void testDeviceLookup() {
        deviceAuthService.register("TEST-ESP32-005", "Node 5", "mykey", "ESP32", "DHT11");
        Optional<DeviceRegistration> found = deviceAuthService.findById("TEST-ESP32-005");
        assertThat(found).isPresent();
        assertThat(found.get().getSensorType()).isEqualTo("DHT11");
    }
}
