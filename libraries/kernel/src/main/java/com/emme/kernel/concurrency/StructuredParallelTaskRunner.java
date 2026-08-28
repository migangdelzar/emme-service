package com.emme.kernel.concurrency;

import com.emme.kernel.context.AiExecutionContextBridge;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Stream;

/** Java 25 structured-concurrency implementation of {@link ParallelTaskRunner}. */
@SuppressWarnings("preview")
public final class StructuredParallelTaskRunner implements ParallelTaskRunner {

  @Override
  public <T> List<T> runRequired(List<? extends Callable<T>> tasks, Deadline deadline) {
    List<Callable<T>> validatedTasks = validate(tasks, deadline);
    if (validatedTasks.isEmpty()) {
      return List.of();
    }
    Duration timeout = remaining(deadline);

    try (StructuredTaskScope<T, Stream<StructuredTaskScope.Subtask<T>>> scope =
        StructuredTaskScope.open(
            StructuredTaskScope.Joiner.<T>allSuccessfulOrThrow(),
            configuration -> configuration.withTimeout(timeout))) {
      List<StructuredTaskScope.Subtask<T>> subtasks = forkAll(scope, validatedTasks);
      scope.join();
      return subtasks.stream().map(StructuredTaskScope.Subtask::get).toList();
    } catch (StructuredTaskScope.TimeoutException exception) {
      throw new ParallelTaskTimeoutException(exception);
    } catch (StructuredTaskScope.FailedException exception) {
      throw new ParallelTaskExecutionException(exception.getCause());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ParallelTaskInterruptedException(exception);
    }
  }

  @Override
  public <T> List<TaskOutcome<T>> runOptional(
      List<? extends Callable<T>> tasks, Deadline deadline) {
    List<Callable<T>> validatedTasks = validate(tasks, deadline);
    if (validatedTasks.isEmpty()) {
      return List.of();
    }
    Duration timeout = remaining(deadline);

    try (StructuredTaskScope<T, Void> scope =
        StructuredTaskScope.open(
            StructuredTaskScope.Joiner.<T>awaitAll(),
            configuration -> configuration.withTimeout(timeout))) {
      List<StructuredTaskScope.Subtask<T>> subtasks = forkAll(scope, validatedTasks);
      scope.join();
      return outcomes(subtasks);
    } catch (StructuredTaskScope.TimeoutException exception) {
      throw new ParallelTaskTimeoutException(exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ParallelTaskInterruptedException(exception);
    }
  }

  @Override
  public <T> Optional<T> runFirstSuccessful(List<? extends Callable<T>> tasks, Deadline deadline) {
    List<Callable<T>> validatedTasks = validate(tasks, deadline);
    if (validatedTasks.isEmpty()) {
      return Optional.empty();
    }
    Duration timeout = remaining(deadline);

    try (StructuredTaskScope<T, T> scope =
        StructuredTaskScope.open(
            StructuredTaskScope.Joiner.<T>anySuccessfulResultOrThrow(),
            configuration -> configuration.withTimeout(timeout))) {
      forkAll(scope, validatedTasks);
      return Optional.ofNullable(scope.join());
    } catch (StructuredTaskScope.TimeoutException exception) {
      throw new ParallelTaskTimeoutException(exception);
    } catch (StructuredTaskScope.FailedException exception) {
      if (exception.getCause() instanceof Error error) {
        throw error;
      }
      return Optional.empty();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ParallelTaskInterruptedException(exception);
    }
  }

  private static <T> List<Callable<T>> validate(
      List<? extends Callable<T>> tasks, Deadline deadline) {
    Objects.requireNonNull(tasks, "tasks must not be null");
    Objects.requireNonNull(deadline, "deadline must not be null");
    tasks.forEach(task -> Objects.requireNonNull(task, "task must not be null"));
    return List.copyOf(tasks);
  }

  @SuppressWarnings("preview")
  private static <T> List<StructuredTaskScope.Subtask<T>> forkAll(
      StructuredTaskScope<T, ?> scope, List<Callable<T>> tasks) {
    List<StructuredTaskScope.Subtask<T>> subtasks = new ArrayList<>(tasks.size());
    for (Callable<T> task : tasks) {
      Callable<T> contextualTask =
          AiExecutionContextScope.current().isPresent()
              ? AiExecutionContextBridge.captureCurrent(task)
              : task;
      subtasks.add(scope.fork(contextualTask));
    }
    return subtasks;
  }

  private static Duration remaining(Deadline deadline) {
    Duration remaining = deadline.remaining();
    if (remaining.isNegative() || remaining.isZero()) {
      throw new ParallelTaskTimeoutException();
    }
    return remaining;
  }

  @SuppressWarnings("preview")
  private static <T> List<TaskOutcome<T>> outcomes(List<StructuredTaskScope.Subtask<T>> subtasks) {
    return java.util.stream.IntStream.range(0, subtasks.size())
        .mapToObj(index -> outcome(index, subtasks.get(index)))
        .toList();
  }

  @SuppressWarnings("preview")
  private static <T> TaskOutcome<T> outcome(int index, StructuredTaskScope.Subtask<T> subtask) {
    return switch (subtask.state()) {
      case SUCCESS -> TaskOutcome.success(index, subtask.get());
      case FAILED -> TaskOutcome.failure(index, subtask.exception());
      case UNAVAILABLE ->
          TaskOutcome.failure(index, new CancellationException("Task did not complete"));
    };
  }
}
