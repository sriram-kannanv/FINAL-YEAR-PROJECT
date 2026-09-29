package com.fyp.iotbaseline.mqtt;

import com.fyp.iotbaseline.service.MonitoringService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * MQTT Client Configuration using Eclipse Paho.
 *
 * <p>Creates a Paho MQTT client that connects to the embedded Moquette broker
 * (or external Mosquitto if configured). The client both subscribes to incoming
 * sensor data and is used by the simulator to publish readings.</p>
 */
@Configuration
@Slf4j
public class MqttConfig {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.broker.clientId}")
    private String clientId;

    @Value("${mqtt.broker.topic.subscribe}")
    private String subscribeTopic;

    @Value("${mqtt.broker.connection.timeout:30}")
    private int connectionTimeout;

    @Value("${mqtt.broker.keep.alive:60}")
    private int keepAlive;

    @Bean
    @DependsOn("embeddedMqttBroker")
    public MqttClient mqttClient(MqttMessageHandler messageHandler,
                                 MonitoringService monitoringService) throws MqttException {
        MqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(connectionTimeout);
        options.setKeepAliveInterval(keepAlive);
        options.setAutomaticReconnect(true);

        // Callback for connection and message handling
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log.warn("MQTT connection lost: {}", cause.getMessage());
                monitoringService.setMqttConnected(false);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                messageHandler.handleMessage(topic, new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // no-op for QoS 1 delivery confirmation
            }
        });

        // Retry connection — broker may take a moment to start
        int retries = 5;
        for (int i = 0; i < retries; i++) {
            try {
                client.connect(options);
                break;
            } catch (MqttException e) {
                if (i < retries - 1) {
                    log.warn("MQTT connect attempt {} failed, retrying...", i + 1);
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    throw e;
                }
            }
        }

        client.subscribe(subscribeTopic, 1);
        monitoringService.setMqttConnected(true);
        monitoringService.setBrokerRunning(true);

        log.info("==========================================================");
        log.info("  MQTT CLIENT CONNECTED");
        log.info("  Broker: {} [PLAINTEXT — NOT TLS-encrypted]", brokerUrl);
        log.info("  ClientId: {}", clientId);
        log.info("  Subscribed to: {}", subscribeTopic);
        log.info("==========================================================");

        return client;
    }
}
