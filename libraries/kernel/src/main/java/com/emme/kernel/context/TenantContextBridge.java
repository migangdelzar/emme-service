package com.emme.kernel.context;

import com.emme.functional.unchecked.URunnable;
import com.emme.functional.unchecked.USupplier;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Compatibility bridge from the canonical scoped tenant context to legacy request-local holders.
 *
 * <p>New code should depend on {@link TenantExecutionContextScope}. This bridge exists for existing
 * persistence, routing, and logging adapters that still read {@link TenantContext} or correlation
 * {@code ThreadLocal}s. Values are installed only for the duration of the operation and are
 * restored by {@link TenantContextHolder}.
 */
public final class TenantContextBridge {

  private TenantContextBridge() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static TenantExecutionContext requireCurrent() {
    return TenantExecutionContextScope.requireCurrent();
  }

  public static <T> T callCurrent(USupplier<T> action) {
    Objects.requireNonNull(action, "action must not be null");
    TenantExecutionContext context = requireCurrent();
    return TenantContextHolder.withTenantAndCorrelation(
        context.tenantId(), context.databaseId(), context.correlationId(), action);
  }

  public static void runCurrent(URunnable action) {
    Objects.requireNonNull(action, "action must not be null");
    TenantExecutionContext context = requireCurrent();
    TenantContextHolder.withTenantAndCorrelation(
        context.tenantId(), context.databaseId(), context.correlationId(), action);
  }

  public static <T> Callable<T> captureCurrent(Callable<T> action) {
    Objects.requireNonNull(action, "action must not be null");
    TenantExecutionContext context = requireCurrent();
    return () ->
        TenantExecutionContextScope.call(
            context, () -> TenantContextBridge.callCurrent(() -> action.call()));
  }

  public static Runnable captureCurrent(Runnable action) {
    Objects.requireNonNull(action, "action must not be null");
    TenantExecutionContext context = requireCurrent();
    return () ->
        TenantExecutionContextScope.run(
            context, () -> TenantContextBridge.runCurrent(() -> action.run()));
  }
}
