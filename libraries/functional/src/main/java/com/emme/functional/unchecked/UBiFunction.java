package com.emme.functional.unchecked;

import java.util.function.BiFunction;

@FunctionalInterface
public interface UBiFunction<T, U, R> extends BiFunction<T, U, R> {

  @Override
  default R apply(T t, U u) {
    try {
      return applyThrows(t, u);
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  R applyThrows(T t, U u) throws Throwable;
}
