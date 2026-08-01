package com.emme.functional.throwing;

/**
 * A {@link java.util.function.BiPredicate} variant parameterized by exception type.
 *
 * <pre>{@code
 * ThrowingBiPredicate<String, String, IOException> sameFile =
 *     (a, b) -> Files.isSameFile(Path.of(a), Path.of(b));
 * }</pre>
 */
@FunctionalInterface
public interface ThrowingBiPredicate<T, U, E extends Throwable> {
  boolean test(T t, U u) throws E;
}
