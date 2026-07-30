package com.emme.client;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Connection pool for E2E test users. Ensures parallel test executions never share the same user.
 */
public final class E2eUserPool {

  public static final E2eUserPool INSTANCE = new E2eUserPool();

  private static final int POOL_SIZE = Integer.getInteger("e2e.user.pool.size", 10);

  private final Queue<TestUser> available = new ConcurrentLinkedQueue<>();
  private final Set<String> inUse = Collections.synchronizedSet(new HashSet<>());

  private E2eUserPool() {
    for (int i = 0; i < POOL_SIZE; i++) {
      available.add(newTestUser(i));
    }
  }

  static TestUser newTestUser(int idx) {
    return new TestUser(
        "e2e-user-" + idx,
        "e2e-tenant-" + idx,
        "E2E User " + idx,
        "e2e-" + idx + "@emme-e2e.test",
        List.of("platform_admin", "tenant_owner"));
  }

  public synchronized TestUser acquire() {
    // Self-heal: if pool drained but nothing tracked in-use, repopulate
    if (available.isEmpty() && inUse.isEmpty()) {
      for (int i = 0; i < POOL_SIZE; i++) {
        available.add(newTestUser(i));
      }
      System.out.printf("[E2eUserPool] Self-healed: repopulated %d users%n", POOL_SIZE);
    }
    if (available.isEmpty()) {
      throw new IllegalStateException(
          "E2E user pool exhausted. Increase e2e.user.pool.size. In use: " + inUse.size());
    }
    var user = available.poll();
    inUse.add(user.userId());
    System.out.printf(
        "[E2eUserPool] Acquired: %s (%d/%d in use)%n", user.userId(), inUse.size(), POOL_SIZE);
    return user;
  }

  public synchronized void release(String userId) {
    if (!inUse.remove(userId)) {
      System.out.printf("[E2eUserPool] WARN: user %s not in use, skipping release%n", userId);
      return;
    }
    int idx = Integer.parseInt(userId.split("-")[2]);
    available.add(newTestUser(idx));
    System.out.printf(
        "[E2eUserPool] Released: %s (%d/%d in use)%n", userId, inUse.size(), POOL_SIZE);
  }

  public int available() {
    return available.size();
  }

  public int inUse() {
    return inUse.size();
  }

  public int size() {
    return POOL_SIZE;
  }

  public record TestUser(
      String userId, String tenantId, String name, String email, List<String> roles) {}
}
