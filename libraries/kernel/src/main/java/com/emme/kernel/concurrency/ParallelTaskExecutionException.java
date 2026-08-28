package com.emme.kernel.concurrency;

/** Stable failure raised when a required parallel task fails. */
public class ParallelTaskExecutionException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ParallelTaskExecutionException(Throwable cause) {
    super("Parallel task execution failed", cause);
  }
}
