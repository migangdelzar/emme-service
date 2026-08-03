package com.emme.identity.adapter.in.messaging.consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.emme.identity.api.command.ProvisionTenantIdentityCommand;
import com.emme.identity.api.usecase.ProvisionTenantIdentityUseCase;
import com.emme.tenancy.api.event.TenantCreated;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantCreatedConsumerTest {

  @Test
  void delegatesTheCommittedTenantFactToRealmProvisioning() {
    ProvisionTenantIdentityUseCase useCase = mock(ProvisionTenantIdentityUseCase.class);
    TenantCreatedConsumer consumer = new TenantCreatedConsumer(useCase);
    TenantCreated event =
        new TenantCreated(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "studio-a",
            "Studio A",
            "admin@studio-a.emme.app");

    consumer.on(event);

    verify(useCase)
        .provision(
            new ProvisionTenantIdentityCommand(
                event.tenantId(), event.slug(), event.name(), event.adminEmail()));
  }
}
