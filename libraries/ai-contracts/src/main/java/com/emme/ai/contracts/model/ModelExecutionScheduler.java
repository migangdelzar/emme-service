package com.emme.ai.contracts.model;

import com.emme.kernel.context.AiExecutionContext;
import java.time.Duration;
import java.util.concurrent.Callable;

/** Bounded admission port for expensive model operations. */
public interface ModelExecutionScheduler {

  <T> T execute(
      ModelCapability capability,
      AiExecutionContext context,
      Duration admissionTimeout,
      Callable<T> operation);
}
