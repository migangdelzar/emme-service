package com.emme.ai.platform.model;

import com.emme.ai.contracts.context.AiExecutionContext;
import com.emme.ai.contracts.model.ModelCapability;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Fair, bounded admission control for model work.
 *
 * <p>The caller owns the execution thread. In production that caller should be a virtual thread;
 * this class only bounds admission and never creates a model process or an unbounded executor.
 */
public final class BoundedModelExecutionScheduler implements ModelExecutionScheduler {

  private final ModelCapacityProfile profile;
  private final Semaphore globalPermits;
  private final Map<ModelCapability, Semaphore> capabilityPermits;
  private final Map<UUID, Semaphore> tenantPermits = new ConcurrentHashMap<>();
  private final Map<UUID, Semaphore> userPermits = new ConcurrentHashMap<>();
  private final Object queueMonitor = new Object();
  private final Map<UUID, Deque<Waiter<?>>> tenantQueues = new HashMap<>();
  private final Deque<UUID> activeTenants = new ArrayDeque<>();
  private int queuedCount;

  public BoundedModelExecutionScheduler(ModelCapacityProfile profile) {
    this.profile = Objects.requireNonNull(profile, "profile must not be null");
    globalPermits = new Semaphore(profile.globalLimit(), true);
    Semaphore generationPermits = new Semaphore(profile.generationLimit(), true);
    capabilityPermits =
        Map.of(
            ModelCapability.GENERATION,
            generationPermits,
            ModelCapability.VISION,
            generationPermits,
            ModelCapability.EMBEDDING,
            new Semaphore(profile.embeddingLimit(), true));
  }

  int queuedTasks() {
    synchronized (queueMonitor) {
      return queuedCount;
    }
  }

  @Override
  public <T> T execute(
      ModelCapability capability,
      AiExecutionContext context,
      Duration admissionTimeout,
      Callable<T> operation) {
    Objects.requireNonNull(capability, "capability must not be null");
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(admissionTimeout, "admissionTimeout must not be null");
    Objects.requireNonNull(operation, "operation must not be null");
    if (admissionTimeout.isNegative() || admissionTimeout.isZero()) {
      throw new IllegalArgumentException("admissionTimeout must be positive");
    }

    Permit permit = awaitPermit(new Waiter<>(capability, context, admissionTimeout));
    try {
      return operation.call();
    } catch (RuntimeException | Error exception) {
      throw exception;
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    } finally {
      permit.release();
    }
  }

  private Permit awaitPermit(Waiter<?> waiter) {
    long deadline = System.nanoTime() + waiter.timeout.toNanos();
    synchronized (queueMonitor) {
      Permit immediate = tryAcquire(waiter);
      if (immediate != null && queuedCount == 0) {
        return immediate;
      }
      if (immediate != null) {
        immediate.release();
      }
      if (queuedCount >= profile.queueCapacity()) {
        throw new ModelAdmissionRejectedException("Model admission queue is full");
      }
      enqueue(waiter);
      while (true) {
        Permit permit = tryNextPermit(waiter);
        if (permit != null) {
          return permit;
        }
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) {
          remove(waiter);
          throw new ModelAdmissionTimeoutException("Model admission deadline expired");
        }
        try {
          queueMonitor.wait(Math.max(1L, remaining / 1_000_000L), (int) (remaining % 1_000_000L));
        } catch (InterruptedException exception) {
          remove(waiter);
          Thread.currentThread().interrupt();
          throw new ModelAdmissionInterruptedException(exception);
        }
      }
    }
  }

  private Permit tryNextPermit(Waiter<?> requestedWaiter) {
    if (!isHead(requestedWaiter)) {
      return null;
    }
    Permit permit = tryAcquire(requestedWaiter);
    if (permit == null) {
      return null;
    }
    remove(requestedWaiter);
    return permit;
  }

  private boolean isHead(Waiter<?> waiter) {
    Deque<Waiter<?>> queue = tenantQueues.get(waiter.context.tenantId());
    return queue != null
        && queue.peekFirst() == waiter
        && activeTenants.peekFirst().equals(waiter.context.tenantId());
  }

  private Permit tryAcquire(Waiter<?> waiter) {
    if (!globalPermits.tryAcquire()) {
      return null;
    }
    Semaphore capability = capabilityPermits.get(waiter.capability);
    if (!capability.tryAcquire()) {
      globalPermits.release();
      return null;
    }
    Semaphore tenant =
        tenantPermits.computeIfAbsent(
            waiter.context.tenantId(), ignored -> new Semaphore(profile.tenantLimit(), true));
    if (!tenant.tryAcquire()) {
      capability.release();
      globalPermits.release();
      return null;
    }
    Semaphore user =
        userPermits.computeIfAbsent(
            waiter.context.principalId(), ignored -> new Semaphore(profile.userLimit(), true));
    if (!user.tryAcquire()) {
      tenant.release();
      capability.release();
      globalPermits.release();
      return null;
    }
    return new Permit(capability, tenant, user);
  }

  private void enqueue(Waiter<?> waiter) {
    Deque<Waiter<?>> queue =
        tenantQueues.computeIfAbsent(waiter.context.tenantId(), ignored -> new ArrayDeque<>());
    if (queue.isEmpty()) {
      activeTenants.addLast(waiter.context.tenantId());
    }
    queue.addLast(waiter);
    queuedCount++;
  }

  private void remove(Waiter<?> waiter) {
    Deque<Waiter<?>> queue = tenantQueues.get(waiter.context.tenantId());
    if (queue == null || !queue.remove(waiter)) {
      return;
    }
    queuedCount--;
    if (queue.isEmpty()) {
      tenantQueues.remove(waiter.context.tenantId());
      activeTenants.remove(waiter.context.tenantId());
    } else if (activeTenants.peekFirst().equals(waiter.context.tenantId())) {
      activeTenants.removeFirst();
      activeTenants.addLast(waiter.context.tenantId());
    }
    queueMonitor.notifyAll();
  }

  private final class Permit {
    private final Semaphore capability;
    private final Semaphore tenant;
    private final Semaphore user;
    private boolean released;

    private Permit(Semaphore capability, Semaphore tenant, Semaphore user) {
      this.capability = capability;
      this.tenant = tenant;
      this.user = user;
    }

    private void release() {
      synchronized (queueMonitor) {
        if (!released) {
          released = true;
          user.release();
          tenant.release();
          capability.release();
          globalPermits.release();
          queueMonitor.notifyAll();
        }
      }
    }
  }

  private static final class Waiter<T> {
    private final ModelCapability capability;
    private final AiExecutionContext context;
    private final Duration timeout;

    private Waiter(ModelCapability capability, AiExecutionContext context, Duration timeout) {
      this.capability = capability;
      this.context = context;
      this.timeout = timeout;
    }
  }
}
