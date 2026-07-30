package com.emme.functional.unchecked;

import java.util.function.Predicate;

@FunctionalInterface
public interface UPredicate<T> extends Predicate<T> {

    @Override
    default boolean test(T t) {
        try {
            return testThrows(t);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    boolean testThrows(T t) throws Throwable;
}
