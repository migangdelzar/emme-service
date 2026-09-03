package com.emme.assistant.ai.application.trace;

import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import org.slf4j.Logger;

/** Surfaces best-effort trace persistence failures with backend-owned correlation context. */
public final class AiTracePersistenceFailureReporter {

  private AiTracePersistenceFailureReporter() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static void report(Logger logger, String operation, RuntimeException failure) {
    Objects.requireNonNull(logger, "logger must not be null");
    Objects.requireNonNull(operation, "operation must not be null");
    Objects.requireNonNull(failure, "failure must not be null");
    AiExecutionContextScope.current()
        .ifPresentOrElse(
            context -> logWithContext(logger, operation, failure, context),
            () ->
                logger.warn(
                    "AI trace persistence failed operation={} without active execution context",
                    operation,
                    failure));
  }

  private static void logWithContext(
      Logger logger, String operation, RuntimeException failure, AiExecutionContext context) {
    logger.warn(
        "AI trace persistence failed operation={} tenantId={} principalId={} conversationId={} workflowId={} traceId={}",
        operation,
        context.tenantId(),
        context.principalId(),
        context.conversationId(),
        context.workflowId(),
        context.traceId(),
        failure);
  }
}
