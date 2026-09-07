package com.emme.testing.context;

import com.emme.functional.throwing.ThrowingRunnable;
import com.emme.functional.throwing.ThrowingSupplier;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextBridge;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.context.TenantContextHolder;
import java.util.UUID;

/** Shared test helpers for executing work with tenant and AI context bound. */
public final class ExecutionTestContext {

  private ExecutionTestContext() {
    throw new UnsupportedOperationException("Utility class");
  }

  /** Executes work with both the tenant and AI execution context bound. */
  public static <T> T withContext(
      AiExecutionContext context, ThrowingSupplier<T, ? extends Throwable> operation) {
    return withTenant(
        context.tenantId(), () -> AiExecutionContextScope.call(context, operation::get));
  }

  /** Executes a void operation with both the tenant and AI execution context bound. */
  public static void runWithContext(
      AiExecutionContext context, ThrowingRunnable<? extends Throwable> operation) {
    withTenant(context.tenantId(), () -> AiExecutionContextScope.run(context, operation::run));
  }

  /** Executes work with the AI context bridged to legacy tenant and correlation holders. */
  public static <T> T withBridgedContext(
      AiExecutionContext context, ThrowingSupplier<T, ? extends Throwable> operation) {
    return AiExecutionContextScope.call(
        context, () -> AiExecutionContextBridge.callCurrent(operation::get));
  }

  /** Executes a void operation with the AI context bridged to legacy context holders. */
  public static void runWithBridgedContext(
      AiExecutionContext context, ThrowingRunnable<? extends Throwable> operation) {
    AiExecutionContextScope.run(context, () -> AiExecutionContextBridge.runCurrent(operation::run));
  }

  /** Executes work with the supplied tenant context bound. */
  public static <T> T withTenant(
      UUID tenantId, ThrowingSupplier<T, ? extends Throwable> operation) {
    return TenantContextHolder.withTenantOverride(tenantId, operation::get);
  }

  /** Executes a void operation with the supplied tenant context bound. */
  public static void withTenant(UUID tenantId, ThrowingRunnable<? extends Throwable> operation) {
    TenantContextHolder.withTenantOverride(tenantId, operation::run);
  }
}
