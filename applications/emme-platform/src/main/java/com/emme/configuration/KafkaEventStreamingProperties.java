package com.emme.configuration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Typed policy and deployment settings for the optional Kafka event stream. */
@Validated
@ConfigurationProperties(prefix = "app.messaging.kafka")
public record KafkaEventStreamingProperties(
    boolean enabled,
    @NotBlank String bootstrapServers,
    @NotBlank String securityProtocol,
    @Min(1) int producerRetries,
    @NotBlank String consumerGroup,
    @NotBlank String saslMechanism,
    String saslJaasConfig) {

  public KafkaEventStreamingProperties(
      @DefaultValue("false") boolean enabled,
      @DefaultValue("localhost:9092") String bootstrapServers,
      @DefaultValue("PLAINTEXT") String securityProtocol,
      @DefaultValue("10") int producerRetries,
      @DefaultValue("emme-platform") String consumerGroup,
      @DefaultValue("PLAIN") String saslMechanism,
      @DefaultValue("") String saslJaasConfig) {
    this.enabled = enabled;
    this.bootstrapServers = bootstrapServers;
    this.securityProtocol = securityProtocol;
    this.producerRetries = producerRetries;
    this.consumerGroup = consumerGroup;
    this.saslMechanism = saslMechanism;
    this.saslJaasConfig = saslJaasConfig == null ? "" : saslJaasConfig;
  }

  public static KafkaEventStreamingProperties defaults() {
    return new KafkaEventStreamingProperties(
        false, "localhost:9092", "PLAINTEXT", 10, "emme-platform", "PLAIN", "");
  }

  /** Returns whether the configured stream can be enabled in a production profile. */
  @AssertTrue(
      message =
          "Enabled production Kafka streaming requires a non-local bootstrap and encrypted transport")
  public boolean isSafeForProduction() {
    if (!enabled) {
      return true;
    }

    boolean secureTransport =
        !bootstrapServers.startsWith("localhost:")
            && !"PLAINTEXT".equalsIgnoreCase(securityProtocol);
    boolean saslConfigured =
        !securityProtocol.toUpperCase(Locale.ROOT).startsWith("SASL_") || !saslJaasConfig.isBlank();
    return secureTransport && saslConfigured;
  }
}
