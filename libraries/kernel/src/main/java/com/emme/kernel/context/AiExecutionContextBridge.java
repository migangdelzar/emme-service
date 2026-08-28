package com.emme.kernel.context;

import com.emme.functional.unchecked.URunnable;
import com.emme.functional.unchecked.USupplier;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Adapts the backend-authenticated AI context to legacy tenant and correlation holders.
 *
 * <p>The scoped AI context remains the authority. This bridge only installs compatible
 * ThreadLocal/MDC values for existing application and persistence adapters and restores the
 * previous values when the operation completes.
 */
public final class AiExecutionContextBridge {

  private AiExecutionContextBridge() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static <T> T callCurrent(USupplier<T> action) {
    Objects.requireNonNull(action, "action must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    return TenantContextHolder.withTenantAndCorrelation(
        context.tenantId(), context.traceId(), action);
  }

  public static void runCurrent(URunnable action) {
    Objects.requireNonNull(action, "action must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    TenantContextHolder.withTenantAndCorrelation(context.tenantId(), context.traceId(), action);
  }

  public static <T> Callable<T> captureCurrent(Callable<T> action) {
    Objects.requireNonNull(action, "action must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    return () ->
        AiExecutionContextScope.call(
            context, () -> AiExecutionContextBridge.callCurrent(() -> action.call()));
  }
}
