package com.emme.identity.adapter.out.ratelimit;

import com.emme.identity.application.port.out.LoginAttemptRateLimiter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-local fallback used when no distributed store is configured.
 *
 * <p>Production deployments should provide the Redis implementation so the limit is shared by all
 * service instances.
 */
public class InMemoryLoginAttemptRateLimiter implements LoginAttemptRateLimiter {

  private final ConcurrentHashMap<String, List<Long>> attempts = new ConcurrentHashMap<>();

  @Override
  public boolean tryAcquire(String key, int maxAttempts, long windowMs) {
    long now = System.currentTimeMillis();
    List<Long> timestamps =
        attempts.computeIfAbsent(key, ignored -> Collections.synchronizedList(new ArrayList<>()));

    synchronized (timestamps) {
      timestamps.removeIf(timestamp -> now - timestamp > windowMs);
      if (timestamps.size() >= maxAttempts) {
        return false;
      }
      timestamps.add(now);
      return true;
    }
  }
}
