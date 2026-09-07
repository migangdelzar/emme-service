package com.emme.identity.adapter.out.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisLoginAttemptRateLimiterLiveTest {

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:8.10.1-alpine3.23"))
          .withExposedPorts(6379);

  @Test
  void failsClosedDuringRedisOutageAndRecoversAfterRedisRestart() {
    String key = "identity:login:live:" + System.nanoTime();
    StringRedisTemplate redis = redisTemplate();
    RedisLoginAttemptRateLimiter limiter = new RedisLoginAttemptRateLimiter(redis);

    assertThat(limiter.tryAcquire(key, 2, 60_000L)).isTrue();
    assertThat(redis.getExpire(key, TimeUnit.MILLISECONDS)).isPositive();

    REDIS.stop();

    assertThat(limiter.tryAcquire(key, 2, 60_000L)).isFalse();

    REDIS.start();
    StringRedisTemplate recoveredRedis = redisTemplate();
    assertThat(new RedisLoginAttemptRateLimiter(recoveredRedis).tryAcquire(key, 2, 60_000L))
        .isTrue();
  }

  private static StringRedisTemplate redisTemplate() {
    LettuceConnectionFactory connectionFactory =
        new LettuceConnectionFactory(
            new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getFirstMappedPort()),
            LettuceClientConfiguration.builder().commandTimeout(Duration.ofSeconds(1)).build());
    connectionFactory.afterPropertiesSet();
    StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
    template.afterPropertiesSet();
    return template;
  }
}
