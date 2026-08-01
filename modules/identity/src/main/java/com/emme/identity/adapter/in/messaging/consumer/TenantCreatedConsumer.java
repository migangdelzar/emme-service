package com.emme.identity.adapter.in.messaging.consumer;

import com.emme.identity.application.process.KeycloakRealmProvisioningProcessManager;
import com.emme.tenancy.api.event.TenantCreated;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Consumes tenant-created facts and starts Identity realm provisioning. */
@Component
public class TenantCreatedConsumer {

  private final KeycloakRealmProvisioningProcessManager processManager;

  public TenantCreatedConsumer(KeycloakRealmProvisioningProcessManager processManager) {
    this.processManager = processManager;
  }

  @EventListener
  @Transactional
  public void on(TenantCreated event) {
    processManager.provision(event);
  }
}
