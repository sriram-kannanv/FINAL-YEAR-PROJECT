package com.fyp.iotbaseline.mqtt;

import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Properties;

/**
 * Embedded Moquette MQTT Broker Configuration.
 *
 * <p>This starts a Moquette MQTT broker embedded within the Spring Boot
 * application. It serves as a drop-in replacement for a standalone
 * Mosquitto installation for the local Review 2 demonstration.</p>
 *
 * <p><b>NOTE:</b> This broker runs on {@code tcp://localhost:1883}
 * WITHOUT TLS — plaintext only. This correctly represents the
 * "MQTT without TLS" scenario common in conventional IoT deployments
 * as described in Chapter 3 of the report.</p>
 *
 * <p>To use a real Mosquitto broker instead:
 * <ol>
 *   <li>Set {@code mqtt.embedded.broker.enabled=false} in application.properties</li>
 *   <li>Install Mosquitto and start: {@code mosquitto -c mosquitto.conf}</li>
 *   <li>Update {@code mqtt.broker.url} accordingly</li>
 * </ol></p>
 */
@Configuration
@ConditionalOnProperty(name = "mqtt.embedded.broker.enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddedBrokerConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedBrokerConfig.class);

    @Value("${mqtt.embedded.broker.port:1883}")
    private int brokerPort;

    @Value("${mqtt.embedded.broker.host:localhost}")
    private String brokerHost;

    @Value("${mqtt.embedded.broker.data.dir:./mqtt-data}")
    private String dataDir;

    private Server mqttBroker;

    public Server getMqttBroker() {
        return mqttBroker;
    }

    @Bean(name = "embeddedMqttBroker")
    public Server startEmbeddedBroker() throws IOException {
        Properties config = new Properties();
        config.setProperty("host", brokerHost);
        config.setProperty("port", String.valueOf(brokerPort));
        config.setProperty("allow_anonymous", "true");
        config.setProperty("data_path", dataDir);
        // TLS explicitly disabled — plaintext only for local demo (no ssl_port set)

        mqttBroker = new Server();
        mqttBroker.startServer(new MemoryConfig(config));

        log.info("============================================================");
        log.info("  EMBEDDED MQTT BROKER STARTED");
        log.info("  Host: {}:{}", brokerHost, brokerPort);
        log.info("  TLS:  DISABLED (plaintext — localhost only)");
        log.info("  Type: Moquette v0.17 (Mosquitto replacement for local demo)");
        log.info("  To use Mosquitto: set mqtt.embedded.broker.enabled=false");
        log.info("============================================================");

        return mqttBroker;
    }

    @PreDestroy
    public void stopBroker() {
        if (mqttBroker != null) {
            mqttBroker.stopServer();
            log.info("Embedded MQTT broker stopped.");
        }
    }
}
