package com.emme.testing.integration.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.testcontainers.containers.PostgreSQLContainer;

class PostgresContainerConfigurationTest {

  @Test
  void createsNonReusableContainersForIsolatedIntegrationRuns() {
    PostgreSQLContainer<?> container = new PostgresContainerConfiguration().postgresContainer();

    assertThat(container.getImage().toString()).contains("imageName=pgvector/pgvector:pg16");
    assertThat(container.isShouldBeReused()).isFalse();
  }

  @Test
  void publishesJdbcConnectionDetailsForTheApplicationDatasourceBoundary() {
    PostgreSQLContainer<?> container = mock(PostgreSQLContainer.class);
    when(container.getJdbcUrl()).thenReturn("jdbc:postgresql://localhost:5432/emme_test");
    when(container.getUsername()).thenReturn("emme");
    when(container.getPassword()).thenReturn("secret");

    JdbcConnectionDetails details =
        new PostgresContainerConfiguration().postgresJdbcConnectionDetails(container);

    assertThat(details.getJdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/emme_test");
    assertThat(details.getUsername()).isEqualTo("emme");
    assertThat(details.getPassword()).isEqualTo("secret");
  }

  @Test
  void ordersPublicationRegistryShutdownBeforeThePostgresContainer() {
    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.registerBeanDefinition(
        "eventPublicationRegistry", new RootBeanDefinition(Object.class));
    beanFactory.registerBeanDefinition("postgresContainer", new RootBeanDefinition(Object.class));
    beanFactory.registerBeanDefinition(
        "tenantDatabasePoolProvider", new RootBeanDefinition(Object.class));

    PostgresContainerConfiguration.eventPublicationRegistryShutdownOrdering()
        .postProcessBeanFactory(beanFactory);

    assertThat(beanFactory.getBeanDefinition("eventPublicationRegistry").getDependsOn())
        .containsExactlyInAnyOrder("postgresContainer", "tenantDatabasePoolProvider");
  }
}
