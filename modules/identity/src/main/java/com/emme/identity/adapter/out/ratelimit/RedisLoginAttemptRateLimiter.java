package com.emme.identity.adapter.out.ratelimit;

import com.emme.identity.application.port.out.LoginAttemptRateLimiter;
import java.util.List;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/** Redis-backed atomic login-attempt limiter shared by service instances. */
public class RedisLoginAttemptRateLimiter implements LoginAttemptRateLimiter {

  private static final RedisScript<Long> ACQUIRE_SCRIPT =
      new DefaultRedisScript<>(
          """
          local count = redis.call('INCR', KEYS[1])
          if count == 1 then
            redis.call('PEXPIRE', KEYS[1], ARGV[1])
          end
          return count
          """,
          Long.class);

  private final StringRedisTemplate redis;

  public RedisLoginAttemptRateLimiter(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public boolean tryAcquire(String key, int maxAttempts, long windowMs) {
    try {
      Long count = redis.execute(ACQUIRE_SCRIPT, List.of(key), Long.toString(windowMs));
      return count != null && count <= maxAttempts;
    } catch (RedisConnectionFailureException ignored) {
      return false;
    }
  }
}
