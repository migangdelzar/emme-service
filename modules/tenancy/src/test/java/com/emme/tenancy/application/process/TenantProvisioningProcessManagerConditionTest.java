package com.emme.tenancy.application.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TenantProvisioningProcessManagerConditionTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withBean(
              TenantProvisioningRepository.class, () -> mock(TenantProvisioningRepository.class))
          .withBean(TenantSchemaMigrationPort.class, () -> mock(TenantSchemaMigrationPort.class))
          .withUserConfiguration(TenantProvisioningProcessManager.class);

  @Test
  void doesNotCreateDatabaseBackedSchedulerWhenProvisioningIsDisabled() {
    contextRunner
        .withPropertyValues("app.tenant.provisioning.enabled=false")
        .run(
            context -> assertThat(context).doesNotHaveBean(TenantProvisioningProcessManager.class));
  }

  @Test
  void createsDatabaseBackedSchedulerByDefault() {
    contextRunner.run(
        context -> assertThat(context).hasSingleBean(TenantProvisioningProcessManager.class));
  }
}
