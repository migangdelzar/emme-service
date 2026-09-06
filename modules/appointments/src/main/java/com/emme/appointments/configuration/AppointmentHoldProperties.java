package com.emme.appointments.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for tenant-local appointment holds. */
@ConfigurationProperties("app.ai.appointment-workflow")
public record AppointmentHoldProperties(Duration holdDuration) {

  public AppointmentHoldProperties {
    if (holdDuration == null) {
      holdDuration = Duration.ofMinutes(15);
    }
    if (holdDuration.isZero() || holdDuration.isNegative()) {
      throw new IllegalArgumentException("holdDuration must be positive");
    }
  }
}
