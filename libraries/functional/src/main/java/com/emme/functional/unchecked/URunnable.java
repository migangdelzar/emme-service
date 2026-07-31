package com.emme.functional.unchecked;

@FunctionalInterface
public interface URunnable extends Runnable {

  @Override
  default void run() {
    try {
      runThrows();
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  void runThrows() throws Throwable;
}
