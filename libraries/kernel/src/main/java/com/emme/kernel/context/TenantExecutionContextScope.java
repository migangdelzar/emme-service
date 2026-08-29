package com.emme.kernel.context;

import com.emme.functional.unchecked.URunnable;
import com.emme.functional.unchecked.USupplier;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Lexically binds the backend-authenticated tenant context.
 *
 * <p>{@link ScopedValue} is inherited by structured subtasks. Ordinary executor submissions must
 * use one of the capture methods so the immutable context is explicitly captured at submission
 * time.
 */
public final class TenantExecutionContextScope {

  private static final ScopedValue<TenantExecutionContext> CURRENT = ScopedValue.newInstance();

  private TenantExecutionContextScope() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static Optional<TenantExecutionContext> current() {
    return CURRENT.isBound() ? Optional.of(CURRENT.get()) : Optional.empty();
  }

  public static TenantExecutionContext requireCurrent() {
    if (!CURRENT.isBound()) {
      throw new IllegalStateException("No tenant execution context");
    }
    return CURRENT.get();
  }

  public static <T> T call(TenantExecutionContext context, USupplier<T> action) {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(action, "action must not be null");
    try {
      return ScopedValue.where(CURRENT, context).call(action::get);
    } catch (RuntimeException | Error exception) {
      throw exception;
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  public static void run(TenantExecutionContext context, URunnable action) {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(action, "action must not be null");
    ScopedValue.where(CURRENT, context).run(action);
  }

  public static <T> Callable<T> captureCurrent(Callable<T> action) {
    TenantExecutionContext context = requireCurrent();
    Objects.requireNonNull(action, "action must not be null");
    return () -> call(context, () -> action.call());
  }

  public static Runnable captureCurrent(Runnable action) {
    TenantExecutionContext context = requireCurrent();
    Objects.requireNonNull(action, "action must not be null");
    return () -> run(context, () -> action.run());
  }
}
