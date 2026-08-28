package com.emme.kernel.context;

import com.emme.functional.unchecked.URunnable;
import com.emme.functional.unchecked.USupplier;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Lexical binding and executor bridge for the current AI execution context.
 *
 * <p>{@link ScopedValue} is inherited by structured subtasks. Ordinary executor submissions must
 * use {@link #captureCurrent(Callable)} or {@link #captureCurrent(Runnable)} so that context is
 * explicitly captured at submission time.
 */
public final class AiExecutionContextScope {

  private static final ScopedValue<AiExecutionContext> CURRENT = ScopedValue.newInstance();

  private AiExecutionContextScope() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static Optional<AiExecutionContext> current() {
    return CURRENT.isBound() ? Optional.of(CURRENT.get()) : Optional.empty();
  }

  public static AiExecutionContext requireCurrent() {
    if (!CURRENT.isBound()) {
      throw new IllegalStateException("No AI execution context");
    }
    return CURRENT.get();
  }

  public static <T> T call(AiExecutionContext context, USupplier<T> action) {
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

  public static void run(AiExecutionContext context, URunnable action) {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(action, "action must not be null");
    ScopedValue.where(CURRENT, context).run(action);
  }

  public static <T> Callable<T> captureCurrent(Callable<T> action) {
    AiExecutionContext context = requireCurrent();
    Objects.requireNonNull(action, "action must not be null");
    return () -> call(context, () -> action.call());
  }

  public static Runnable captureCurrent(Runnable action) {
    AiExecutionContext context = requireCurrent();
    Objects.requireNonNull(action, "action must not be null");
    return () -> run(context, action::run);
  }
}
