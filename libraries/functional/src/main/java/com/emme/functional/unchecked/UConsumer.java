package com.emme.functional.unchecked;

import java.util.function.Consumer;

@FunctionalInterface
public interface UConsumer<T> extends Consumer<T> {

  @Override
  default void accept(T t) {
    try {
      acceptThrows(t);
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  void acceptThrows(T t) throws Throwable;
}
