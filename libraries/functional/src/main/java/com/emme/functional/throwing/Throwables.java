package com.emme.functional.throwing;

import com.emme.functional.unchecked.UBiConsumer;
import com.emme.functional.unchecked.UBiFunction;
import com.emme.functional.unchecked.UBiPredicate;
import com.emme.functional.unchecked.UConsumer;
import com.emme.functional.unchecked.UFunction;
import com.emme.functional.unchecked.UPredicate;
import com.emme.functional.unchecked.URunnable;
import com.emme.functional.unchecked.USupplier;

/**
 * Bridges parameterized {@code Throwing*} interfaces to the unchecked {@code U*} wrappers.
 *
 * <pre>{@code
 * ThrowingFunction<String, URI, URISyntaxException> parser = URI::new;
 * stream.map(Throwables.wrapFunction(parser));
 * }</pre>
 */
public final class Throwables {

  private Throwables() {
    throw new UnsupportedOperationException();
  }

  public static <E extends Throwable> URunnable wrapRunnable(ThrowingRunnable<E> f) {
    return f::run;
  }

  public static <T, E extends Throwable> USupplier<T> wrapSupplier(ThrowingSupplier<T, E> f) {
    return f::get;
  }

  public static <T, R, E extends Throwable> UFunction<T, R> wrapFunction(
      ThrowingFunction<T, R, E> f) {
    return f::apply;
  }

  public static <T, E extends Throwable> UConsumer<T> wrapConsumer(ThrowingConsumer<T, E> f) {
    return f::accept;
  }

  public static <T, U, E extends Throwable> UBiConsumer<T, U> wrapBiConsumer(
      ThrowingBiConsumer<T, U, E> f) {
    return f::accept;
  }

  public static <T, U, R, E extends Throwable> UBiFunction<T, U, R> wrapBiFunction(
      ThrowingBiFunction<T, U, R, E> f) {
    return f::apply;
  }

  public static <T, E extends Throwable> UPredicate<T> wrapPredicate(ThrowingPredicate<T, E> f) {
    return f::test;
  }

  public static <T, U, E extends Throwable> UBiPredicate<T, U> wrapBiPredicate(
      ThrowingBiPredicate<T, U, E> f) {
    return f::test;
  }
}
