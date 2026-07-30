package com.emme.functional.unchecked;

import java.util.function.Supplier;

@FunctionalInterface
public interface USupplier<T> extends Supplier<T> {

    @Override
    default T get() {
        try {
            return getThrows();
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    T getThrows() throws Throwable;
}
