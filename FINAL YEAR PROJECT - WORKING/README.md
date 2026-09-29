# IoT Baseline System — Existing System (Review 2)

> **Adaptive Source-Level Encryption and Security Management Framework for ESP32-Based IoT Data Transmission and Cloud Storage**  
> *This module implements ONLY the existing-system baseline described in Chapter 3 of the project report.*

---

## 🗂 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Prerequisites & Installation](#prerequisites--installation)
5. [Running the Application](#running-the-application)
6. [Sensor Simulator](#sensor-simulator)
7. [REST API Reference](#rest-api-reference)
8. [Database Details](#database-details)
9. [Dashboard Guide](#dashboard-guide)
10. [Security Scope & Limitations](#security-scope--limitations)
11. [Demonstration Workflow (Review 2)](#demonstration-workflow-review-2)
12. [Existing System Advantages & Limitations](#existing-system-advantages--limitations)
13. [Extension to Proposed System](#extension-to-proposed-system)
14. [Completed vs. Pending Functionality](#completed-vs-pending-functionality)

---

## Overview

This is the **conventional IoT-cloud baseline system** for the final-year project demonstration. It implements the standard architecture described in Chapter 3 of the project report — without any proposed system enhancements.

**What this system demonstrates:**

```
Sensor Simulator (ESP32) → MQTT Broker → Spring Boot Backend → SQLite → Web Dashboard
```

The system is fully self-contained on a single laptop. No physical ESP32 hardware, no paid cloud services, and no external MQTT broker installation are required.

---

## Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                  Conventional IoT-Cloud Baseline                    │
├────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────┐    MQTT (plaintext)    ┌──────────────────┐   │
│  │  Sensor         │  ──────────────────►   │  Embedded MQTT   │   │
│  │  Simulator      │  Topic:                │  Broker          │   │
│  │  (ESP32/DHT22)  │  iot/sensor/{id}/data  │  (Moquette)      │   │
│  │                 │                        │  tcp://localhost  │   │
│  │  • Temperature  │                        │  :1883           │   │
│  │  • Humidity     │  ⚠ NO TLS (demo only) │                  │   │
│  │  • Timestamp    │                        └────────┬─────────┘   │
│  │  • Device ID    │                                 │              │
│  │  • Secret Key   │                                 │ subscribe    │
│  └─────────────────┘                                 ▼              │
│                                               ┌──────────────────┐  │
│                                               │  Spring Boot     │  │
│                                               │  Backend         │  │
│                                               │  • Auth Service  │  │
│                                               │  • Msg Handler   │  │
│                                               │  • REST API      │  │
│                                               └────────┬─────────┘  │
│                                                        │             │
│                                                        ▼             │
│                                               ┌──────────────────┐  │
│                                               │  SQLite          │  │
│                                               │  iot_baseline.db │  │
│                                               │  • sensor_       │  │
│                                               │    readings      │  │
│                                               │  • device_       │  │
│                                               │    registrations │  │
│                                               └────────┬─────────┘  │
│                                                        │             │
│                                                        ▼             │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │              Web Dashboard (Browser)                          │   │
│  │  Login → KPI Cards → Charts → Device Status → Simulator Ctrl  │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
└────────────────────────────────────────────────────────────────────┘
```

### Conventional Workflow (Chapter 3)

| Step | Component | Description |
|------|-----------|-------------|
| 1 | Device Registration | Basic deviceId + SHA-256 hashed secret key |
| 2 | Sensor Data Collection | DHT22 temperature and humidity simulation |
| 3 | Data Preprocessing | JSON payload with metadata |
| 4 | Device Authentication | Shared key verification at backend |
| 5 | Network Transmission | MQTT over TCP (no TLS for local demo) |
| 6 | Backend Reception | Spring Boot service validates and processes |
| 7 | Storage | SQLite (represents cloud storage layer) |
| 8 | Monitoring | Device availability, MQTT status, rejection counters |
| 9 | Dashboard Access | Username/password protected web UI |

---

## Technology Stack

| Component | Technology | Notes |
|-----------|-----------|-------|
| Language | Java 17+ | Compiled at Java 17 language level |
| Framework | Spring Boot 3.3 | Web, JPA, Security, Thymeleaf |
| Build | Maven 3.9 | |
| MQTT Broker | Moquette 0.17 (embedded) | Replaces external Mosquitto; swap-in documented below |
| MQTT Client | Eclipse Paho 1.2.5 | |
| Database | SQLite via Hibernate | `iot_baseline.db` in project root |
| Frontend | Thymeleaf + Vanilla JS | Chart.js for graphs |
| Security | Spring Security 6 | Form login, BCrypt passwords |
| Testing | JUnit 5 + Spring Test | |

---

## Prerequisites & Installation

### Required

| Tool | Minimum Version | Check |
|------|----------------|-------|
| Java JDK | 17 | `java --version` |
| Apache Maven | 3.6 | `mvn --version` |
| Internet (first run) | — | Maven downloads ~100 MB dependencies |

### NOT Required

- Physical ESP32 hardware (software simulator included)
- Mosquitto or any external MQTT broker (Moquette embedded broker starts automatically)
- Cloud accounts (AWS, Azure, GCP — SQLite used locally)
- Python or Node.js

### Clone / Setup

```bash
# Already in your project directory — no cloning needed.
# Just navigate to the project root:
cd "d:\PROFESSIONAL\PROJECTS\FINAL YEAR PROJECT - WORKING"
```

---

## Running the Application

### Start the Application

```bash
mvn spring-boot:run
```

On first run, Maven downloads all dependencies (~100 MB). This takes 2–5 minutes.

**What starts:**
1. ✅ Embedded Moquette MQTT broker on `tcp://localhost:1883`
2. ✅ Spring Boot backend on `http://localhost:8080`
3. ✅ SQLite database created as `./iot_baseline.db`
4. ✅ Default demo device `ESP32-DEMO-001` auto-registered

**Expected startup output:**
```
EMBEDDED MQTT BROKER STARTED
  Host: localhost:1883
  TLS:  DISABLED (plaintext — localhost only)

MQTT CLIENT CONNECTED
  Broker: tcp://localhost:1883 [PLAINTEXT — NOT TLS-encrypted]
  Subscribed to: iot/sensor/#

Default demo device registered: ESP32-DEMO-001 | Secret: demo-secret-key-2024
```

### Access the Dashboard

Open your browser: **http://localhost:8080**

Login credentials:
- **Username:** `admin`
- **Password:** `admin123`

---

## Using a Real Mosquitto Broker (Optional)

To swap the embedded Moquette broker for a real Mosquitto installation:

1. Install Mosquitto: https://mosquitto.org/download/
2. Edit `src/main/resources/application.properties`:
   ```properties
   mqtt.embedded.broker.enabled=false
   mqtt.broker.url=tcp://localhost:1883
   ```
3. Start Mosquitto: `mosquitto -v`
4. Run the application: `mvn spring-boot:run`

---

## Sensor Simulator

The simulator mimics an ESP32 microcontroller with a DHT22 temperature/humidity sensor.

### Via Dashboard

1. Click **Start** in the Simulator panel
2. Adjust the interval slider (2–60 seconds)
3. Watch readings appear in KPI cards, charts, and the readings table

### Via REST API

```bash
# Start with 5-second interval
curl -X POST http://localhost:8080/api/simulator/start \
  -H "Content-Type: application/json" \
  -d '{"intervalSeconds": 5}'

# Stop
curl -X POST http://localhost:8080/api/simulator/stop

# Status
curl http://localhost:8080/api/simulator/status
```

### Simulator Payload (published to MQTT)

```json
{
  "deviceId":    "ESP32-DEMO-001",
  "deviceName":  "Demo ESP32 Sensor Node",
  "secretKey":   "demo-secret-key-2024",
  "temperature": 27.3,
  "humidity":    61.5,
  "timestamp":   "2024-01-15T10:30:00",
  "simulated":   true,
  "sensorType":  "DHT22",
  "unit_temp":   "Celsius",
  "unit_humidity": "Percent",
  "encryption":  "NONE"
}
```

> ⚠️ The `"encryption": "NONE"` field explicitly documents that the conventional system transmits data **in plaintext** — no source-level encryption. This is the core limitation addressed by the proposed system.

---

## REST API Reference

Base URL: `http://localhost:8080`

All API endpoints are **publicly accessible** (no auth) to support dashboard JS polling and simulator integration.

### Simulator

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/simulator/status` | Get simulator running state and stats |
| POST | `/api/simulator/start` | Start simulator (body: `{"intervalSeconds": 5}`) |
| POST | `/api/simulator/stop` | Stop simulator |
| POST | `/api/simulator/interval` | Update interval (body: `{"intervalSeconds": 10}`) |

### Devices

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/devices/register` | Register a new device |
| GET | `/api/devices` | List all registered devices |
| GET | `/api/devices/{deviceId}/status` | Get device status |
| POST | `/api/devices/authenticate` | Test device authentication |

**Register Device Example:**
```bash
curl -X POST http://localhost:8080/api/devices/register \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "ESP32-NODE-002",
    "deviceName": "Lab Sensor 2",
    "secretKey": "my-secret-key",
    "deviceType": "ESP32",
    "sensorType": "DHT22"
  }'
```

**Authenticate Device (test rejection):**
```bash
# Wrong key — should return authenticated: false
curl -X POST http://localhost:8080/api/devices/authenticate \
  -H "Content-Type: application/json" \
  -d '{"deviceId": "ESP32-DEMO-001", "secretKey": "wrong-key"}'
```

### Sensor Readings

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/sensors/readings` | Paged readings (optional: `?deviceId=&page=0&size=20`) |
| GET | `/api/sensors/readings/latest` | Most recent reading (optional: `?deviceId=`) |
| GET | `/api/sensors/readings/{deviceId}/history` | History with date range |
| GET | `/api/sensors/readings/chart` | Chart data (optional: `?hours=24&deviceId=`) |
| GET | `/api/sensors/stats` | Total and rejected reading counts |

### Monitoring

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/monitoring/status` | Full system status (MQTT, broker, DB) |
| GET | `/api/monitoring/stats` | Dashboard KPI summary |

---

## Database Details

**Type:** SQLite  
**File:** `./iot_baseline.db` (auto-created in project root)  
**Note:** This local SQLite database **represents the cloud storage layer** for the local demonstration. In a production deployment, this would be replaced by a managed cloud database (AWS RDS, Azure SQL, Google Cloud Firestore, etc.).

### Schema

**`device_registrations`**
| Column | Type | Notes |
|--------|------|-------|
| device_id | TEXT (PK) | Unique device identifier |
| device_name | TEXT | Human-readable name |
| hashed_secret_key | TEXT | SHA-256 hash of the secret key |
| device_type | TEXT | "ESP32" by default |
| sensor_type | TEXT | "DHT22" by default |
| registered_at | DATETIME | Registration timestamp |
| last_seen | DATETIME | Last successful communication |
| status | TEXT | REGISTERED / ACTIVE / INACTIVE / NEVER_CONNECTED |
| total_readings | INTEGER | Count of accepted readings |
| rejected_messages | INTEGER | Count of rejected auth attempts |

**`sensor_readings`**
| Column | Type | Notes |
|--------|------|-------|
| id | INTEGER (PK) | Auto-incremented |
| device_id | TEXT | FK to device |
| device_name | TEXT | Denormalized for query efficiency |
| temperature | REAL | °C from DHT22 |
| humidity | REAL | % from DHT22 |
| timestamp | DATETIME | Sensor reading time |
| received_at | DATETIME | Backend reception time |
| mqtt_topic | TEXT | MQTT topic it arrived on |
| raw_payload | TEXT | Original JSON (plaintext — no encryption) |
| simulated | BOOLEAN | true if from software simulator |
| quality | TEXT | VALID / AUTH_FAILED / INVALID_RANGE |

### Inspect the Database

```bash
# If SQLite CLI is installed:
sqlite3 iot_baseline.db ".tables"
sqlite3 iot_baseline.db "SELECT * FROM sensor_readings ORDER BY timestamp DESC LIMIT 10;"
sqlite3 iot_baseline.db "SELECT * FROM device_registrations;"
```

---

## Dashboard Guide

1. **Login page** — Enter `admin` / `admin123`
2. **KPI Cards** — Live counters: total readings, latest temp/humidity, active devices, rejected messages
3. **Charts** — Temperature and humidity history graphs; filter by time range (1h/6h/24h) and device
4. **Recent Readings Table** — Paged table; filter by device; shows source (simulated/device) and quality
5. **Device Status Panel** — All registered devices with status dot (green=active, red=inactive), last-seen timestamp, reading counts
6. **Simulator Controls** — Start/stop, interval slider, published/failed counters
7. **Register Device** — Form to add new devices
8. **System Status** — MQTT, broker, TLS, database status with notes on what's active/inactive

---

## Security Scope & Limitations

### Implemented (Conventional Baseline — Chapter 3)

| Mechanism | Implementation |
|-----------|--------------|
| Device authentication | Shared secret key, SHA-256 hashed at registration |
| Dashboard access control | Spring Security form login, BCrypt |
| MQTT topic routing | Device-specific topics (`iot/sensor/{deviceId}/data`) |
| Basic input validation | Sensor value range checking, required field validation |
| Device availability monitoring | Last-seen timestamp comparison |

### NOT Implemented (Proposed System — deliberately excluded)

| Feature | Reason Not Included |
|---------|-------------------|
| AES-256-GCM source-level encryption | Proposed system enhancement only |
| Adaptive security policy engine | Proposed system |
| ECDHE / certificate-based auth | Proposed system |
| Advanced key management | Proposed system |
| Anomaly / threat detection | Proposed system |
| Incident management framework | Proposed system |
| Active TLS for MQTT | Template provided; not activated for localhost demo |

### Transport Security Note

> **TLS is NOT active** for this local demonstration. The MQTT connection runs on `tcp://localhost:1883` (plaintext). This is explicitly labelled in the dashboard and logs.  
>   
> **Difference between TLS and source-level encryption:**  
> - **TLS** encrypts data *in transit* between the device and broker — but the broker and backend see plaintext.  
> - **Source-level encryption (AES-256-GCM)** encrypts data *at the sensor*, so it remains ciphertext even inside the broker and backend, until decrypted by an authorized party. The proposed system implements this.

---

## Demonstration Workflow (Review 2)

Follow these steps to demonstrate the complete existing system:

```
Step 1: Start the application
  mvn spring-boot:run
  [Wait for "MQTT CLIENT CONNECTED" and "Tomcat started on port 8080"]

Step 2: Open dashboard
  http://localhost:8080
  Login: admin / admin123

Step 3: Verify system status
  → System Status panel shows: Broker=Online, MQTT=Connected, TLS=Not Active, DB=Online

Step 4: Start the simulator
  → Click "Start" in Simulator panel (5-second interval)
  → KPI cards update with temperature and humidity values
  → Readings appear in the table
  → Charts draw data points

Step 5: Demonstrate device authentication
  → Open a terminal and run:
    curl -X POST http://localhost:8080/api/devices/authenticate \
      -H "Content-Type: application/json" \
      -d '{"deviceId":"ESP32-DEMO-001","secretKey":"wrong-key"}'
  → Response: {"authenticated": false, ...}
  → Rejected count increments in dashboard

Step 6: Register a second device
  → Use "Register Device" form in dashboard
  → New device appears in Device Status panel

Step 7: Show SQLite persistence
  → Stop and restart the application (mvn spring-boot:run)
  → Previous readings still appear in the dashboard (persisted in SQLite)

Step 8: Stop simulator
  → Click "Stop"
  → After ~60 seconds, device status changes to "INACTIVE"
```

---

## Existing System Advantages & Limitations

### Advantages (Chapter 3)

- **Simplicity** — Standard MQTT + REST architecture; easy to deploy and maintain
- **Low cost** — Minimal infrastructure; open-source components
- **Interoperability** — MQTT is a universal IoT protocol; works with any MQTT-compatible device
- **Established ecosystem** — Spring Boot, SQLite, Chart.js are mature, well-documented technologies
- **Rapid prototyping** — Device registration and data flow can be set up quickly
- **Scalability** (with cloud upgrade) — The architecture can be scaled by replacing SQLite with a cloud database

### Limitations (Chapter 3)

1. **No source-level encryption** — Sensor data is transmitted and stored in plaintext. An attacker who can intercept MQTT messages (even with TLS, inside the broker/network) sees raw sensor values.

2. **Weak authentication** — Shared secret key authentication is vulnerable to: credential theft, replay attacks (no nonce/timestamp validation), brute-force attacks (no rate limiting), and lacks mutual authentication.

3. **No key management** — Keys are static and never rotated. There is no key derivation, certificate lifecycle, or secure key provisioning.

4. **Variable TLS adoption** — In many conventional deployments, TLS is not configured. This demonstration correctly reflects that by running without TLS (localhost only).

5. **No threat detection** — The system cannot detect anomalous sensor patterns, replay attacks, or unusual access patterns. Monitoring is basic availability-only.

6. **No incident response** — There is no alerting, incident logging, or automated response to security events.

7. **No data integrity verification** — Payload tampering cannot be detected (no HMAC or digital signatures on the data itself).

8. **Centralised trust** — The backend must be fully trusted. Data integrity depends entirely on the backend's security.

---

## Extension to Proposed System

The existing system provides a clean baseline for demonstrating the limitations that the proposed system addresses:

| Existing System | Proposed System Extension |
|----------------|--------------------------|
| Plaintext MQTT payload | AES-256-GCM encryption at ESP32 source |
| Shared secret key auth | ECDHE certificate-based mutual authentication |
| Static credentials | Adaptive key rotation and derivation (HKDF) |
| No threat detection | Integrated anomaly detection engine |
| Basic availability monitoring | Security event monitoring and alerting |
| Manual configuration | Adaptive security policy engine |
| No incident management | Centralised incident management framework |
| TLS optional | TLS mandatory + source-level encryption |

---

## Completed vs. Pending Functionality

### ✅ Completed (this module)

- [x] Embedded MQTT broker (Moquette) — automatic startup
- [x] Sensor simulator (ESP32/DHT22) with start/stop/interval controls
- [x] Device registration with SHA-256 hashed credentials
- [x] Basic device authentication (reject invalid credentials)
- [x] MQTT message reception and payload validation
- [x] Sensor range validation (temperature −40 to 80°C, humidity 0–100%)
- [x] SQLite persistence (readings + device records)
- [x] REST API (devices, sensors, simulator, monitoring)
- [x] Spring Security dashboard login
- [x] Monitoring dashboard with live data
- [x] Chart.js temperature and humidity history graphs
- [x] Device status monitoring (active/inactive detection)
- [x] Rejection counter and quality tagging
- [x] Integration tests (registration, auth, rejection)

### ⚡ Simulated (not physical hardware)

- [ ] Physical ESP32 microcontroller (replaced by software simulator)
- [ ] Real DHT22 sensor (simulated with realistic drift values)
- [ ] Remote cloud storage (replaced by local SQLite)
- [ ] Active TLS certificate (configuration template provided)

### 🚫 Deliberately Excluded (proposed system)

- [ ] AES-256-GCM source-level encryption
- [ ] Adaptive security policy engine
- [ ] Advanced cryptographic key management
- [ ] Integrated threat detection
- [ ] Incident management framework
- [ ] Certificate-based mutual authentication
