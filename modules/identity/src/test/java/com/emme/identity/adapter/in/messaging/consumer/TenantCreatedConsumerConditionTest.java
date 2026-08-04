package com.emme.identity.adapter.in.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.identity.api.usecase.ProvisionTenantIdentityUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TenantCreatedConsumerConditionTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withBean(
              ProvisionTenantIdentityUseCase.class,
              () -> mock(ProvisionTenantIdentityUseCase.class))
          .withUserConfiguration(TenantCreatedConsumer.class);

  @Test
  void doesNotCreateConsumerWhenProvisioningPropertyIsMissing() {
    contextRunner.run(context -> assertThat(context).doesNotHaveBean(TenantCreatedConsumer.class));
  }

  @Test
  void doesNotCreateConsumerWhenProvisioningIsDisabled() {
    contextRunner
        .withPropertyValues("app.keycloak.provisioning.enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(TenantCreatedConsumer.class));
  }

  @Test
  void createsConsumerWhenProvisioningIsExplicitlyEnabled() {
    contextRunner
        .withPropertyValues("app.keycloak.provisioning.enabled=true")
        .run(context -> assertThat(context).hasSingleBean(TenantCreatedConsumer.class));
  }
}
