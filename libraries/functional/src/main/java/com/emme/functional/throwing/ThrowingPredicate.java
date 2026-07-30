package com.emme.functional.throwing;

@FunctionalInterface
public interface ThrowingPredicate<T, E extends Throwable> {
    boolean test(T t) throws E;
}
