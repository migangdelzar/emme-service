package com.emme.functional.unchecked;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface UBiConsumer<T, U> extends BiConsumer<T, U> {

  @Override
  default void accept(T t, U u) {
    try {
      acceptThrows(t, u);
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  void acceptThrows(T t, U u) throws Throwable;
}
