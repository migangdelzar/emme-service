package com.emme.kernel.concurrency;

import java.util.Optional;

/** Result of one task in an optional parallel operation. */
public record TaskOutcome<T>(int index, T result, Throwable error) {

  public static <T> TaskOutcome<T> success(int index, T value) {
    return new TaskOutcome<>(index, value, null);
  }

  public static <T> TaskOutcome<T> failure(int index, Throwable failure) {
    return new TaskOutcome<>(index, null, failure);
  }

  public boolean isSuccess() {
    return error == null;
  }

  public Optional<T> value() {
    return Optional.ofNullable(result);
  }

  public Optional<Throwable> failure() {
    return Optional.ofNullable(error);
  }
}
