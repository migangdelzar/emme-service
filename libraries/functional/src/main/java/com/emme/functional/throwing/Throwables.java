package com.emme.functional.throwing;

import com.emme.functional.unchecked.*;

/**
 * Bridges parameterized {@code Throwing*} interfaces to
 * the unchecked {@code U*} wrappers.
 * <pre>{@code
 * ThrowingFunction<String, URI, URISyntaxException> parser = URI::new;
 * stream.map(Throwables.wrap(parser));
 * }</pre>
 */
public final class Throwables {

    private Throwables() { throw new UnsupportedOperationException(); }

    public static <E extends Throwable> URunnable wrap(ThrowingRunnable<E> f) {
        return f::run;
    }

    public static <T, E extends Throwable> USupplier<T> wrap(ThrowingSupplier<T, E> f) {
        return f::get;
    }

    public static <T, R, E extends Throwable> UFunction<T, R> wrap(ThrowingFunction<T, R, E> f) {
        return f::apply;
    }

    public static <T, E extends Throwable> UConsumer<T> wrap(ThrowingConsumer<T, E> f) {
        return f::accept;
    }

    public static <T, U, E extends Throwable> UBiConsumer<T, U> wrap(ThrowingBiConsumer<T, U, E> f) {
        return f::accept;
    }

    public static <T, U, R, E extends Throwable> UBiFunction<T, U, R> wrap(ThrowingBiFunction<T, U, R, E> f) {
        return f::apply;
    }

    public static <T, E extends Throwable> UPredicate<T> wrap(ThrowingPredicate<T, E> f) {
        return f::test;
    }

    public static <T, U, E extends Throwable> UBiPredicate<T, U> wrap(ThrowingBiPredicate<T, U, E> f) {
        return f::test;
    }
}
