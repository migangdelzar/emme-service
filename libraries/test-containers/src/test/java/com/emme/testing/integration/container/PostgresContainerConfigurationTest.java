package com.emme.testing.integration.container;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.testcontainers.containers.PostgreSQLContainer;

class PostgresContainerConfigurationTest {

  @Test
  void createsNonReusableContainersForIsolatedIntegrationRuns() {
    PostgreSQLContainer<?> container = new PostgresContainerConfiguration().postgresContainer();

    assertThat(container.isShouldBeReused()).isFalse();
  }

  @Test
  void ordersPublicationRegistryShutdownBeforeThePostgresContainer() {
    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.registerBeanDefinition(
        "eventPublicationRegistry", new RootBeanDefinition(Object.class));
    beanFactory.registerBeanDefinition("postgresContainer", new RootBeanDefinition(Object.class));

    PostgresContainerConfiguration.eventPublicationRegistryShutdownOrdering()
        .postProcessBeanFactory(beanFactory);

    assertThat(beanFactory.getBeanDefinition("eventPublicationRegistry").getDependsOn())
        .containsExactly("postgresContainer");
  }
}
