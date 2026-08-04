package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.configuration.KafkaEventStreamingProperties;
import org.junit.jupiter.api.Test;

class KafkaEventStreamingPropertiesTest {

  @Test
  void isDisabledAndLocalSafeByDefault() {
    KafkaEventStreamingProperties properties = KafkaEventStreamingProperties.defaults();

    assertThat(properties.enabled()).isFalse();
    assertThat(properties.bootstrapServers()).isEqualTo("localhost:9092");
    assertThat(properties.securityProtocol()).isEqualTo("PLAINTEXT");
    assertThat(properties.producerRetries()).isEqualTo(10);
    assertThat(properties.consumerGroup()).isEqualTo("emme-platform");
    assertThat(properties.isSafeForProduction()).isTrue();
  }

  @Test
  void rejectsEnabledLocalPlaintextConfigurationForProduction() {
    KafkaEventStreamingProperties properties =
        new KafkaEventStreamingProperties(true, "localhost:9092", "PLAINTEXT", 10, "emme-platform");

    assertThat(properties.isSafeForProduction()).isFalse();
  }

  @Test
  void acceptsEnabledSecureRemoteConfiguration() {
    KafkaEventStreamingProperties properties =
        new KafkaEventStreamingProperties(
            true, "kafka.example.internal:9093", "SASL_SSL", 10, "emme-platform");

    assertThat(properties.isSafeForProduction()).isTrue();
  }
}
