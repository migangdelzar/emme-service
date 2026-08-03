package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.configuration.KafkaEventStreamingProperties;
import org.junit.jupiter.api.Test;

class KafkaEventStreamingPropertiesTest {

  @Test
  void isDisabledAndLocalSafeByDefault() {
    KafkaEventStreamingProperties properties = new KafkaEventStreamingProperties();

    assertThat(properties.isEnabled()).isFalse();
    assertThat(properties.getBootstrapServers()).isEqualTo("localhost:9092");
    assertThat(properties.getSecurityProtocol()).isEqualTo("PLAINTEXT");
    assertThat(properties.getProducerRetries()).isEqualTo(10);
    assertThat(properties.getConsumerGroup()).isEqualTo("emme-platform");
    assertThat(properties.isSafeForProduction()).isTrue();
  }

  @Test
  void rejectsEnabledLocalPlaintextConfigurationForProduction() {
    KafkaEventStreamingProperties properties = new KafkaEventStreamingProperties();
    properties.setEnabled(true);

    assertThat(properties.isSafeForProduction()).isFalse();
  }

  @Test
  void acceptsEnabledSecureRemoteConfiguration() {
    KafkaEventStreamingProperties properties = new KafkaEventStreamingProperties();
    properties.setEnabled(true);
    properties.setBootstrapServers("kafka.example.internal:9093");
    properties.setSecurityProtocol("SASL_SSL");

    assertThat(properties.isSafeForProduction()).isTrue();
  }
}
