package com.emme.identity.configuration;

import com.emme.identity.adapter.out.ratelimit.InMemoryLoginAttemptRateLimiter;
import com.emme.identity.adapter.out.ratelimit.RedisLoginAttemptRateLimiter;
import com.emme.identity.application.port.out.LoginAttemptRateLimiter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Selects distributed login-rate-limit state when Redis is available. */
@Configuration
public class IdentityRateLimitConfiguration {

  @Bean
  @ConditionalOnBean(StringRedisTemplate.class)
  LoginAttemptRateLimiter redisLoginAttemptRateLimiter(StringRedisTemplate redis) {
    return new RedisLoginAttemptRateLimiter(redis);
  }

  @Bean
  @ConditionalOnMissingBean(LoginAttemptRateLimiter.class)
  LoginAttemptRateLimiter inMemoryLoginAttemptRateLimiter() {
    return new InMemoryLoginAttemptRateLimiter();
  }
}
