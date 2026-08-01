package com.emme.identity.adapter.in.messaging.consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.emme.identity.application.process.KeycloakRealmProvisioningProcessManager;
import com.emme.tenancy.api.event.TenantCreated;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantCreatedConsumerTest {

  @Test
  void delegatesTheCommittedTenantFactToRealmProvisioning() {
    KeycloakRealmProvisioningProcessManager processManager =
        mock(KeycloakRealmProvisioningProcessManager.class);
    TenantCreatedConsumer consumer = new TenantCreatedConsumer(processManager);
    TenantCreated event =
        new TenantCreated(UUID.randomUUID(), "studio-a", "Studio A", "admin@studio-a.emme.app");

    consumer.on(event);

    verify(processManager).provision(event);
  }
}
