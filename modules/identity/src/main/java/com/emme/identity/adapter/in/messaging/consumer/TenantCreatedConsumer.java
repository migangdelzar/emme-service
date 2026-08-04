package com.emme.identity.adapter.in.messaging.consumer;

import com.emme.identity.api.command.ProvisionTenantIdentityCommand;
import com.emme.identity.api.usecase.ProvisionTenantIdentityUseCase;
import com.emme.tenancy.api.event.TenantCreated;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Consumes tenant-created facts and starts Identity realm provisioning. */
@Component
@ConditionalOnProperty(
    name = "app.keycloak.provisioning.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class TenantCreatedConsumer {

  private final ProvisionTenantIdentityUseCase useCase;

  public TenantCreatedConsumer(ProvisionTenantIdentityUseCase useCase) {
    this.useCase = useCase;
  }

  @ApplicationModuleListener
  public void on(TenantCreated event) {
    useCase.provision(
        new ProvisionTenantIdentityCommand(
            event.tenantId(), event.slug(), event.name(), event.adminEmail()));
  }
}
