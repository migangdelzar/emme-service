package com.emme.kernel.concurrency;

/** Stable failure raised when a parallel operation exceeds its deadline. */
public final class ParallelTaskTimeoutException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ParallelTaskTimeoutException() {
    super("Parallel task execution exceeded its deadline");
  }

  public ParallelTaskTimeoutException(Throwable cause) {
    super("Parallel task execution exceeded its deadline", cause);
  }
}
