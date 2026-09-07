package com.emme.identity.adapter.out.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.lettuce.core.RedisException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

class RedisLoginAttemptRateLimiterTest {

  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
  private final RedisLoginAttemptRateLimiter limiter = new RedisLoginAttemptRateLimiter(redis);

  @Test
  void allowsAnAttemptWhenAtomicRedisCountIsWithinTheLimit() {
    when(redis.execute(anyScript(), eq(List.of("identity:login")), eq("60000"))).thenReturn(3L);

    assertThat(limiter.tryAcquire("identity:login", 5, 60_000L)).isTrue();
  }

  @Test
  void rejectsAnAttemptWhenAtomicRedisCountExceedsTheLimit() {
    when(redis.execute(anyScript(), eq(List.of("identity:login")), eq("60000"))).thenReturn(6L);

    assertThat(limiter.tryAcquire("identity:login", 5, 60_000L)).isFalse();
  }

  @Test
  void rejectsAnAttemptWhenRedisIsUnavailable() {
    when(redis.execute(anyScript(), eq(List.of("identity:login")), eq("60000")))
        .thenThrow(new RedisConnectionFailureException("Redis unavailable"));

    assertThat(limiter.tryAcquire("identity:login", 5, 60_000L)).isFalse();
  }

  @Test
  void rejectsAnAttemptWhenRedisCommandTimesOut() {
    when(redis.execute(anyScript(), eq(List.of("identity:login")), eq("60000")))
        .thenThrow(new org.springframework.dao.QueryTimeoutException("Redis command timed out"));

    assertThat(limiter.tryAcquire("identity:login", 5, 60_000L)).isFalse();
  }

  @Test
  void rejectsAnAttemptWhenRedisResetsTheConnection() {
    when(redis.execute(anyScript(), eq(List.of("identity:login")), eq("60000")))
        .thenThrow(new RedisSystemException("Redis connection reset", new RedisException("reset")));

    assertThat(limiter.tryAcquire("identity:login", 5, 60_000L)).isFalse();
  }

  @SuppressWarnings("unchecked")
  private static RedisScript<Long> anyScript() {
    return (RedisScript<Long>) any(RedisScript.class);
  }
}
