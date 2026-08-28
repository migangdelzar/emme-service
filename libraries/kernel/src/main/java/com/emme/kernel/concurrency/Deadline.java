package com.emme.kernel.concurrency;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Absolute deadline shared by a bounded group of concurrent tasks. */
public record Deadline(Instant expiresAt) {

  public Deadline {
    Objects.requireNonNull(expiresAt, "expiresAt must not be null");
  }

  public static Deadline after(Duration duration) {
    Objects.requireNonNull(duration, "duration must not be null");
    if (duration.isNegative() || duration.isZero()) {
      throw new IllegalArgumentException("duration must be greater than zero");
    }
    return new Deadline(Instant.now().plus(duration));
  }

  public Duration remaining() {
    return Duration.between(Instant.now(), expiresAt);
  }
}
