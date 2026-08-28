package com.emme.kernel.concurrency;

/** Stable failure raised when the owner thread is interrupted while joining tasks. */
public final class ParallelTaskInterruptedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ParallelTaskInterruptedException(Throwable cause) {
    super("Parallel task execution was interrupted", cause);
  }
}
