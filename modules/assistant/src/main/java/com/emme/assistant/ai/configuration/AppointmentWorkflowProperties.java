package com.emme.assistant.ai.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for the hold-first appointment workflow boundary. */
@ConfigurationProperties("app.ai.appointment-workflow")
public record AppointmentWorkflowProperties(Duration holdDuration) {

  public AppointmentWorkflowProperties {
    if (holdDuration == null) {
      holdDuration = Duration.ofMinutes(15);
    }
    if (holdDuration.isZero() || holdDuration.isNegative()) {
      throw new IllegalArgumentException("holdDuration must be positive");
    }
  }
}
