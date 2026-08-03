package com.emme.functional.unchecked;

import java.util.function.Function;

@FunctionalInterface
public interface UFunction<T, R> extends Function<T, R> {

  @Override
  default R apply(T t) {
    try {
      return applyThrows(t);
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  R applyThrows(T t) throws Throwable;
}
