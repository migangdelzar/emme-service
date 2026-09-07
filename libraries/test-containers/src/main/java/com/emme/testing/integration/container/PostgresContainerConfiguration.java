package com.emme.testing.integration.container;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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

  private static final String IMAGE = "pgvector/pgvector:pg16";
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

  @Bean
  JdbcConnectionDetails postgresJdbcConnectionDetails(PostgreSQLContainer<?> container) {
    return new ContainerJdbcConnectionDetails(
        container.getJdbcUrl(), container.getUsername(), container.getPassword());
  }

  /**
   * Keeps the JDBC publication registry alive until after tenant pools and the PostgreSQL container
   * are available.
   *
   * <p>Spring destroys dependent beans before their dependencies. Making the publication registry
   * depend on both resources guarantees that its outstanding-publication callback runs before the
   * tenant pool provider or Testcontainers closes the backing connection.
   */
  @Bean
  static BeanFactoryPostProcessor eventPublicationRegistryShutdownOrdering() {
    return beanFactory -> {
      if (beanFactory.containsBeanDefinition("eventPublicationRegistry")) {
        var publicationRegistry = beanFactory.getBeanDefinition("eventPublicationRegistry");
        var dependencies = new java.util.ArrayList<String>();
        if (beanFactory.containsBeanDefinition("postgresContainer")) {
          dependencies.add("postgresContainer");
        }
        if (beanFactory.containsBeanDefinition("tenantDatabasePoolProvider")) {
          dependencies.add("tenantDatabasePoolProvider");
        }
        if (!dependencies.isEmpty()) {
          publicationRegistry.setDependsOn(dependencies.toArray(String[]::new));
        }
      }
    };
  }

  private record ContainerJdbcConnectionDetails(String jdbcUrl, String username, String password)
      implements JdbcConnectionDetails {

    @Override
    public String getJdbcUrl() {
      return jdbcUrl;
    }

    @Override
    public String getUsername() {
      return username;
    }

    @Override
    public String getPassword() {
      return password;
    }
  }
}
