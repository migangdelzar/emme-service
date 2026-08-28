package com.emme.kernel.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class StructuredParallelTaskRunnerTest {

  private final ParallelTaskRunner runner = new StructuredParallelTaskRunner();

  @Test
  void runsRequiredTasksInParallelPreservesOrderAndInheritsAiContext() {
    AiExecutionContext context = context();
    CountDownLatch started = new CountDownLatch(2);

    List<String> result =
        AiExecutionContextScope.call(
            context,
            () ->
                runner.runRequired(
                    List.of(task("first", started, context), task("second", started, context)),
                    Deadline.after(Duration.ofSeconds(2))));

    assertThat(started.getCount()).isZero();
    assertThat(result).containsExactly("first", "second");
  }

  @Test
  void requiredTasksExposeFailuresAsAStableRunnerException() {
    IllegalStateException failure = new IllegalStateException("model failed");

    assertThatThrownBy(
            () ->
                runner.runRequired(
                    List.of(
                        () -> {
                          throw failure;
                        }),
                    Deadline.after(Duration.ofSeconds(1))))
        .isInstanceOf(ParallelTaskExecutionException.class)
        .hasCause(failure);
  }

  @Test
  void optionalTasksReturnEachSuccessAndFailureWithoutFailingTheBatch() {
    IllegalArgumentException failure = new IllegalArgumentException("optional failure");

    List<TaskOutcome<String>> outcomes =
        runner.runOptional(
            List.of(
                () -> "available",
                () -> {
                  throw failure;
                }),
            Deadline.after(Duration.ofSeconds(1)));

    assertThat(outcomes).hasSize(2);
    assertThat(outcomes.get(0).isSuccess()).isTrue();
    assertThat(outcomes.get(0).value()).contains("available");
    assertThat(outcomes.get(1).isSuccess()).isFalse();
    assertThat(outcomes.get(1).failure()).contains(failure);
  }

  @Test
  void firstSuccessfulTaskReturnsTheFirstAvailableResult() {
    Optional<String> result =
        runner.runFirstSuccessful(
            List.of(
                () -> {
                  throw new IllegalStateException("local provider unavailable");
                },
                () -> "fallback provider result"),
            Deadline.after(Duration.ofSeconds(1)));

    assertThat(result).contains("fallback provider result");
  }

  @Test
  void firstSuccessfulTaskReturnsEmptyWhenAllTasksFail() {
    Optional<String> result =
        runner.runFirstSuccessful(
            List.of(
                () -> {
                  throw new IllegalStateException("first failure");
                },
                () -> {
                  throw new IllegalArgumentException("second failure");
                }),
            Deadline.after(Duration.ofSeconds(1)));

    assertThat(result).isEmpty();
  }

  @Test
  void firstSuccessfulTaskDoesNotSwallowFatalErrors() {
    AssertionError fatalError = new AssertionError("fatal task error");

    assertThatThrownBy(
            () ->
                runner.runFirstSuccessful(
                    List.of(
                        () -> {
                          throw fatalError;
                        }),
                    Deadline.after(Duration.ofSeconds(1))))
        .isSameAs(fatalError);
  }

  @Test
  void firstSuccessfulTaskCancelsOutstandingTasksAfterAResultArrives() throws Exception {
    CountDownLatch slowTaskStarted = new CountDownLatch(1);
    CountDownLatch slowTaskInterrupted = new CountDownLatch(1);

    Optional<String> result =
        runner.runFirstSuccessful(
            List.of(
                () -> {
                  slowTaskStarted.countDown();
                  try {
                    Thread.sleep(Duration.ofSeconds(5));
                    return "slow";
                  } catch (InterruptedException exception) {
                    slowTaskInterrupted.countDown();
                    throw exception;
                  }
                },
                () -> {
                  assertThat(slowTaskStarted.await(1, TimeUnit.SECONDS)).isTrue();
                  return "fast";
                }),
            Deadline.after(Duration.ofSeconds(2)));

    assertThat(result).contains("fast");
    assertThat(slowTaskInterrupted.await(1, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void cancelsTheScopeWhenTheDeadlineExpires() {
    assertThatThrownBy(
            () ->
                runner.runRequired(
                    List.of(
                        () -> {
                          Thread.sleep(Duration.ofSeconds(5));
                          return "late";
                        }),
                    Deadline.after(Duration.ofMillis(50))))
        .isInstanceOf(ParallelTaskTimeoutException.class);
  }

  @Test
  void rejectsNullTaskCollectionsAndDeadlines() {
    assertThatThrownBy(() -> runner.runRequired(null, Deadline.after(Duration.ofSeconds(1))))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> runner.runRequired(List.of(() -> "value"), null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void returnsEmptyResultsForNoTasks() {
    Deadline deadline = Deadline.after(Duration.ofSeconds(1));

    assertThat(runner.runRequired(List.of(), deadline)).isEmpty();
    assertThat(runner.runOptional(List.of(), deadline)).isEmpty();
    assertThat(runner.runFirstSuccessful(List.of(), deadline)).isEmpty();
  }

  private static Callable<String> task(
      String value, CountDownLatch started, AiExecutionContext context) {
    return () -> {
      assertThat(AiExecutionContextScope.requireCurrent()).isEqualTo(context);
      started.countDown();
      assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
      return value;
    };
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        java.util.Set.of("CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-structured",
        "idempotency-structured");
  }
}
