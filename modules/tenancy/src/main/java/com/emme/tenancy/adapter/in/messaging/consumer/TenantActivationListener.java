package com.emme.tenancy.adapter.in.messaging.consumer;

import com.emme.tenancy.api.event.TenantActivated;
import com.emme.tenancy.api.event.TenantRealmReady;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TenantActivationListener {

  private static final Logger log = LoggerFactory.getLogger(TenantActivationListener.class);

  private final TenantProvisioningRepository provisioningRepository;
  private final ApplicationEventPublisher eventPublisher;

  public TenantActivationListener(
      TenantProvisioningRepository provisioningRepository,
      ApplicationEventPublisher eventPublisher) {
    this.provisioningRepository = provisioningRepository;
    this.eventPublisher = eventPublisher;
  }

  @ApplicationModuleListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onTenantRealmReady(TenantRealmReady event) {
    log.info("Activating tenant {} — schema + realm ready", event.tenantId());

    provisioningRepository.markActive(event.tenantId());

    TenantActivated activated =
        new TenantActivated(
            UUID.randomUUID(),
            event.tenantId(),
            event.slug(),
            provisioningRepository.findSchemaName(event.tenantId()),
            event.keycloakRealm());
    eventPublisher.publishEvent(activated);

    log.info(
        "Tenant {} activated. Schema={}, Realm={}",
        event.tenantId(),
        activated.schemaName(),
        activated.keycloakRealm());
  }
}
