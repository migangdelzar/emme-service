package com.emme.configuration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Typed policy and deployment settings for the optional Kafka event stream. */
@Validated
@ConfigurationProperties(prefix = "app.messaging.kafka")
public class KafkaEventStreamingProperties {

  private boolean enabled;

  @NotBlank private String bootstrapServers = "localhost:9092";

  @NotBlank private String securityProtocol = "PLAINTEXT";

  @Min(1)
  private int producerRetries = 10;

  @NotBlank private String consumerGroup = "emme-platform";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getBootstrapServers() {
    return bootstrapServers;
  }

  public void setBootstrapServers(String bootstrapServers) {
    this.bootstrapServers = bootstrapServers;
  }

  public String getSecurityProtocol() {
    return securityProtocol;
  }

  public void setSecurityProtocol(String securityProtocol) {
    this.securityProtocol = securityProtocol;
  }

  public int getProducerRetries() {
    return producerRetries;
  }

  public void setProducerRetries(int producerRetries) {
    this.producerRetries = producerRetries;
  }

  public String getConsumerGroup() {
    return consumerGroup;
  }

  public void setConsumerGroup(String consumerGroup) {
    this.consumerGroup = consumerGroup;
  }

  /** Returns whether the configured stream can be enabled in a production profile. */
  @AssertTrue(
      message =
          "Enabled production Kafka streaming requires a non-local bootstrap and encrypted transport")
  public boolean isSafeForProduction() {
    return !enabled
        || (!bootstrapServers.startsWith("localhost:")
            && !"PLAINTEXT".equalsIgnoreCase(securityProtocol));
  }
}
