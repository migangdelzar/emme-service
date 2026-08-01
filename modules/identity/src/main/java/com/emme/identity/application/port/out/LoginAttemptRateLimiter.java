package com.emme.identity.application.port.out;

/**
 * Outbound port for atomically reserving a login attempt within a bounded window.
 *
 * <p>Implementations own the state and may use a distributed store. The inbound web filter only
 * decides how to identify the client and how to translate a rejected reservation into HTTP.
 */
@FunctionalInterface
public interface LoginAttemptRateLimiter {

  /**
   * Reserves one attempt when the client has not exceeded the configured limit.
   *
   * @return {@code true} when the attempt is allowed; {@code false} when it is limited
   */
  boolean tryAcquire(String key, int maxAttempts, long windowMs);
}
