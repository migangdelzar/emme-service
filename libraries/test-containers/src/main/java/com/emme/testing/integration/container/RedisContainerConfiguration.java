package com.emme.testing.integration.container;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Optional Redis container. Only activates when {@code emme.testing.redis.enabled=true} is set.
 *
 * <p>Import explicitly via {@code @Import(RedisContainerConfiguration.class)} on tests that require
 * Redis. Not auto-wired by default.
 */
@TestConfiguration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "emme.testing.redis.enabled", havingValue = "true")
public class RedisContainerConfiguration {

  private static final String IMAGE = "redis:8.10.1-alpine3.23";
  private static final int REDIS_PORT = 6379;

  @Bean
  @ServiceConnection
  public GenericContainer<?> redisContainer() {
    return new GenericContainer<>(DockerImageName.parse(IMAGE)).withExposedPorts(REDIS_PORT);
  }
}
