package com.emme.functional.unchecked;

import java.util.function.BiPredicate;

@FunctionalInterface
public interface UBiPredicate<T, U> extends BiPredicate<T, U> {

    @Override
    default boolean test(T t, U u) {
        try {
            return testThrows(t, u);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    boolean testThrows(T t, U u) throws Throwable;
}
