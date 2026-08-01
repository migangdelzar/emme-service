package com.emme.identity.application.port.out;

import java.time.Duration;

/** Time-delay capability used to keep retry orchestration deterministic in tests. */
@FunctionalInterface
public interface RetryDelayPort {

  void await(Duration delay) throws InterruptedException;
}
