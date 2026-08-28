package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.adapter.out.redis.RedisAiLiveEventPublisher;
import com.emme.assistant.ai.adapter.out.redis.RedisAiOperationalStateAdapter;
import com.emme.assistant.ai.application.port.out.AiLiveEventPublisher;
import com.emme.assistant.ai.application.port.out.AiOperationalStatePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Opt-in Redis composition root for temporary AI state, locks, and live events. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.ai.redis", name = "enabled", havingValue = "true")
@ConditionalOnBean(StringRedisTemplate.class)
public class SpringAiRedisConfiguration {

  @Bean
  @ConditionalOnMissingBean
  AiOperationalStatePort aiOperationalStatePort(
      StringRedisTemplate redis, ObjectMapper objectMapper) {
    return new RedisAiOperationalStateAdapter(redis, objectMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  AiLiveEventPublisher aiLiveEventPublisher(StringRedisTemplate redis) {
    return new RedisAiLiveEventPublisher(redis);
  }
}
