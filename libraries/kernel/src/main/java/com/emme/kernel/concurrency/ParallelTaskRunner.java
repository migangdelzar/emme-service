package com.emme.kernel.concurrency;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/** Stable port for bounded parallel task execution. */
public interface ParallelTaskRunner {

  /** Runs all tasks and fails the operation when any task fails or the deadline expires. */
  <T> List<T> runRequired(List<? extends Callable<T>> tasks, Deadline deadline);

  /** Runs all tasks and returns an outcome for each task without failing the whole batch. */
  <T> List<TaskOutcome<T>> runOptional(List<? extends Callable<T>> tasks, Deadline deadline);

  /** Returns the first successful result, or empty when every task fails. */
  <T> Optional<T> runFirstSuccessful(List<? extends Callable<T>> tasks, Deadline deadline);
}
