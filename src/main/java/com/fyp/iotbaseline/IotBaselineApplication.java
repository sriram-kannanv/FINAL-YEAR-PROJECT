package com.fyp.iotbaseline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the IoT Baseline Existing System.
 *
 * <p>This application implements the CONVENTIONAL IoT-cloud architecture
 * described in Chapter 3 of the Final Year Project report:
 * "Adaptive Source-Level Encryption and Security Management Framework
 *  for ESP32-Based IoT Data Transmission and Cloud Storage"</p>
 *
 * <p><b>SCOPE — REVIEW 2 (EXISTING SYSTEM ONLY):</b>
 * This module deliberately omits AES-256-GCM source-level encryption,
 * adaptive security policy engine, advanced key management, and
 * integrated threat detection. Those belong to the PROPOSED SYSTEM
 * and will be implemented separately.</p>
 *
 * <p>Architecture: Sensor Simulator → MQTT (Moquette embedded broker)
 *   → Spring Boot Backend → SQLite → Web Dashboard</p>
 */
@SpringBootApplication
@EnableScheduling
public class IotBaselineApplication {

    public static void main(String[] args) {
        SpringApplication.run(IotBaselineApplication.class, args);
    }
}
