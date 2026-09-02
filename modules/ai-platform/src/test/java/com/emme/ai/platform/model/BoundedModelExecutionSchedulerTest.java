package com.emme.ai.platform.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.ai.contracts.model.ModelCapability;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.kernel.context.AiExecutionContext;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class BoundedModelExecutionSchedulerTest {

  @Test
  void rejectsWhenBoundedQueueIsFull() throws Exception {
    var scheduler = newScheduler(1, 1, 1, 1);
    var started = new CountDownLatch(1);
    var release = new CountDownLatch(1);

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Future<String> first =
          executor.submit(
              () ->
                  scheduler.execute(
                      ModelCapability.GENERATION,
                      context(UUID.randomUUID()),
                      Duration.ofSeconds(5),
                      () -> {
                        started.countDown();
                        release.await();
                        return "first";
                      }));
      assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

      Future<String> queued =
          executor.submit(
              () ->
                  scheduler.execute(
                      ModelCapability.GENERATION,
                      context(UUID.randomUUID()),
                      Duration.ofSeconds(5),
                      () -> "queued"));
      awaitQueued(scheduler, 1);

      assertThatThrownBy(
              () ->
                  scheduler.execute(
                      ModelCapability.GENERATION,
                      context(UUID.randomUUID()),
                      Duration.ofSeconds(1),
                      () -> "rejected"))
          .isInstanceOf(ModelAdmissionRejectedException.class)
          .hasMessageContaining("queue");

      release.countDown();
      assertThat(first.get(2, TimeUnit.SECONDS)).isEqualTo("first");
      assertThat(queued.get(2, TimeUnit.SECONDS)).isEqualTo("queued");
    }
  }

  @Test
  void servesTenantsInRoundRobinOrderWhenTheyAreWaiting() throws Exception {
    var scheduler = newScheduler(1, 4, 1, 1);
    var started = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var order = new CopyOnWriteArrayList<String>();
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Future<String> first =
          executor.submit(
              () ->
                  scheduler.execute(
                      ModelCapability.GENERATION,
                      context(tenantA),
                      Duration.ofSeconds(5),
                      () -> {
                        started.countDown();
                        release.await();
                        order.add("A-0");
                        return "A-0";
                      }));
      assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

      Future<String> a1 = submit(executor, scheduler, tenantA, "A-1", order);
      awaitQueued(scheduler, 1);
      Future<String> a2 = submit(executor, scheduler, tenantA, "A-2", order);
      awaitQueued(scheduler, 2);
      Future<String> b1 = submit(executor, scheduler, tenantB, "B-1", order);
      awaitQueued(scheduler, 3);
      release.countDown();

      first.get(2, TimeUnit.SECONDS);
      a1.get(2, TimeUnit.SECONDS);
      b1.get(2, TimeUnit.SECONDS);
      a2.get(2, TimeUnit.SECONDS);
    }

    assertThat(order).containsExactly("A-0", "A-1", "B-1", "A-2");
  }

  @Test
  void skipsAQueuedTenantThatCannotAcquireItsTenantPermit() throws Exception {
    var scheduler = newScheduler(2, 4, 1, 1);
    var firstStarted = new CountDownLatch(1);
    var releaseFirst = new CountDownLatch(1);
    var tenantBStarted = new CountDownLatch(1);
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      try {
        Future<String> first =
            executor.submit(
                () ->
                    scheduler.execute(
                        ModelCapability.GENERATION,
                        context(tenantA),
                        Duration.ofSeconds(5),
                        () -> {
                          firstStarted.countDown();
                          releaseFirst.await();
                          return "A-0";
                        }));
        assertThat(firstStarted.await(1, TimeUnit.SECONDS)).isTrue();

        Future<String> a1 =
            submit(executor, scheduler, tenantA, "A-1", new CopyOnWriteArrayList<>());
        awaitQueued(scheduler, 1);
        Future<String> b1 =
            executor.submit(
                () ->
                    scheduler.execute(
                        ModelCapability.GENERATION,
                        context(tenantB),
                        Duration.ofSeconds(5),
                        () -> {
                          tenantBStarted.countDown();
                          return "B-1";
                        }));

        assertThat(tenantBStarted.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(b1.get(1, TimeUnit.SECONDS)).isEqualTo("B-1");

        releaseFirst.countDown();
        assertThat(first.get(2, TimeUnit.SECONDS)).isEqualTo("A-0");
        assertThat(a1.get(2, TimeUnit.SECONDS)).isEqualTo("A-1");
      } finally {
        releaseFirst.countDown();
      }
    }
  }

  @Test
  void deadlineExpiryDoesNotConsumeAPermit() throws Exception {
    var scheduler = newScheduler(1, 1, 1, 1);
    var started = new CountDownLatch(1);
    var release = new CountDownLatch(1);

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Future<String> first =
          executor.submit(
              () ->
                  scheduler.execute(
                      ModelCapability.EMBEDDING,
                      context(UUID.randomUUID()),
                      Duration.ofSeconds(5),
                      () -> {
                        started.countDown();
                        release.await();
                        return "first";
                      }));
      assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

      assertThatThrownBy(
              () ->
                  scheduler.execute(
                      ModelCapability.EMBEDDING,
                      context(UUID.randomUUID()),
                      Duration.ofMillis(20),
                      () -> "expired"))
          .isInstanceOf(ModelAdmissionTimeoutException.class);

      release.countDown();
      assertThat(first.get(2, TimeUnit.SECONDS)).isEqualTo("first");
      assertThat(
              scheduler.execute(
                  ModelCapability.EMBEDDING,
                  context(UUID.randomUUID()),
                  Duration.ofSeconds(1),
                  () -> "after-timeout"))
          .isEqualTo("after-timeout");
    }
  }

  @Test
  void enforcesCapabilityLimitsWithoutBlockingOtherCapabilityWork() throws Exception {
    var scheduler = new BoundedModelExecutionScheduler(new ModelCapacityProfile(2, 1, 1, 2, 2, 1));
    var embeddingStarted = new CountDownLatch(1);
    var releaseEmbedding = new CountDownLatch(1);

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Future<String> embedding =
          executor.submit(
              () ->
                  scheduler.execute(
                      ModelCapability.EMBEDDING,
                      context(UUID.randomUUID()),
                      Duration.ofSeconds(5),
                      () -> {
                        embeddingStarted.countDown();
                        releaseEmbedding.await();
                        return "embedding";
                      }));
      assertThat(embeddingStarted.await(1, TimeUnit.SECONDS)).isTrue();

      assertThat(
              scheduler.execute(
                  ModelCapability.GENERATION,
                  context(UUID.randomUUID()),
                  Duration.ofSeconds(1),
                  () -> "generation"))
          .isEqualTo("generation");

      Future<String> secondEmbedding =
          executor.submit(
              () ->
                  scheduler.execute(
                      ModelCapability.EMBEDDING,
                      context(UUID.randomUUID()),
                      Duration.ofMillis(20),
                      () -> "second embedding"));
      assertThatThrownBy(() -> secondEmbedding.get(1, TimeUnit.SECONDS))
          .hasCauseInstanceOf(ModelAdmissionTimeoutException.class);

      releaseEmbedding.countDown();
      assertThat(embedding.get(2, TimeUnit.SECONDS)).isEqualTo("embedding");
    }
  }

  @Test
  void interruptionRemovesWaitingWorkAndLeavesCapacityAvailable() throws Exception {
    var scheduler = newScheduler(1, 1, 1, 1);
    var started = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var interrupted = new AtomicReference<Throwable>();
    var interruptedSignal = new CountDownLatch(1);

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Future<String> first =
          executor.submit(
              () ->
                  scheduler.execute(
                      ModelCapability.GENERATION,
                      context(UUID.randomUUID()),
                      Duration.ofSeconds(5),
                      () -> {
                        started.countDown();
                        release.await();
                        return "first";
                      }));
      assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

      Future<?> waiting =
          executor.submit(
              () -> {
                try {
                  scheduler.execute(
                      ModelCapability.GENERATION,
                      context(UUID.randomUUID()),
                      Duration.ofSeconds(5),
                      () -> "never runs");
                } catch (Throwable failure) {
                  interrupted.set(failure);
                  interruptedSignal.countDown();
                }
              });
      awaitQueued(scheduler, 1);
      waiting.cancel(true);

      assertThat(interruptedSignal.await(1, TimeUnit.SECONDS)).isTrue();
      assertThat(interrupted.get()).isInstanceOf(ModelAdmissionInterruptedException.class);

      release.countDown();
      assertThat(first.get(2, TimeUnit.SECONDS)).isEqualTo("first");
      assertThat(
              scheduler.execute(
                  ModelCapability.GENERATION,
                  context(UUID.randomUUID()),
                  Duration.ofSeconds(1),
                  () -> "after-interruption"))
          .isEqualTo("after-interruption");
    }
  }

  @Test
  void releasesAllPermitsWhenOperationFails() throws Exception {
    var scheduler = newScheduler(1, 0, 1, 1);

    assertThatThrownBy(
            () ->
                scheduler.execute(
                    ModelCapability.GENERATION,
                    context(UUID.randomUUID()),
                    Duration.ofSeconds(1),
                    () -> {
                      throw new IllegalStateException("model failed");
                    }))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("model failed");

    assertThat(
            scheduler.execute(
                ModelCapability.GENERATION,
                context(UUID.randomUUID()),
                Duration.ofSeconds(1),
                () -> "available"))
        .isEqualTo("available");
  }

  private static Future<String> submit(
      java.util.concurrent.ExecutorService executor,
      ModelExecutionScheduler scheduler,
      UUID tenantId,
      String result,
      List<String> order) {
    return executor.submit(
        () ->
            scheduler.execute(
                ModelCapability.GENERATION,
                context(tenantId),
                Duration.ofSeconds(5),
                () -> {
                  order.add(result);
                  return result;
                }));
  }

  private static BoundedModelExecutionScheduler newScheduler(
      int globalLimit, int queueCapacity, int tenantLimit, int userLimit) {
    return new BoundedModelExecutionScheduler(
        new ModelCapacityProfile(
            globalLimit, globalLimit, globalLimit, tenantLimit, userLimit, queueCapacity));
  }

  private static AiExecutionContext context(UUID tenantId) {
    return new AiExecutionContext(
        tenantId,
        UUID.randomUUID(),
        Set.of("CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-" + UUID.randomUUID(),
        "idem-" + UUID.randomUUID());
  }

  private static void awaitQueued(BoundedModelExecutionScheduler scheduler, int expected)
      throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
    while (scheduler.queuedTasks() < expected && System.nanoTime() < deadline) {
      Thread.sleep(1);
    }
    assertThat(scheduler.queuedTasks()).isEqualTo(expected);
  }
}
