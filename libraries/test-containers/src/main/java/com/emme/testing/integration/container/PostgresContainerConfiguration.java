package com.emme.testing.integration.container;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Testcontainers PostgreSQL configuration auto-wired via {@link ServiceConnection}.
 *
 * <p>Only active under the {@code integration-test} profile. Unit/slice tests use H2 and never
 * trigger a container start.
 */
@TestConfiguration(proxyBeanMethods = false)
@Profile("integration-test")
public class PostgresContainerConfiguration {

  private static final String IMAGE = "postgres:16-alpine";
  private static final String DATABASE = "emme_test";
  private static final String USERNAME = "emme";
  private static final String PASSWORD = "emme";

  @Bean
  @ServiceConnection
  public PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>(IMAGE)
        .withDatabaseName(DATABASE)
        .withUsername(USERNAME)
        .withPassword(PASSWORD);
  }

  /**
   * Keeps the JDBC publication registry alive until after the PostgreSQL container is available.
   *
   * <p>Spring destroys dependent beans before their dependencies. Making the publication registry
   * depend on the container therefore guarantees that its outstanding-publication callback runs
   * before Testcontainers stops PostgreSQL.
   */
  @Bean
  static BeanFactoryPostProcessor eventPublicationRegistryShutdownOrdering() {
    return beanFactory -> {
      if (beanFactory.containsBeanDefinition("eventPublicationRegistry")
          && beanFactory.containsBeanDefinition("postgresContainer")) {
        beanFactory
            .getBeanDefinition("eventPublicationRegistry")
            .setDependsOn("postgresContainer");
      }
    };
  }
}
