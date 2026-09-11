package com.emme.tenancy.adapter.in.messaging.consumer;

import com.emme.kernel.context.TenantContextHolder;
import com.emme.tenancy.api.event.TenantActivated;
import com.emme.tenancy.api.event.TenantRealmReady;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class TenantActivationListener {

  private final TenantProvisioningRepository provisioningRepository;
  private final ApplicationEventPublisher eventPublisher;

  public TenantActivationListener(
      TenantProvisioningRepository provisioningRepository,
      ApplicationEventPublisher eventPublisher) {
    this.provisioningRepository = provisioningRepository;
    this.eventPublisher = eventPublisher;
  }

  @ApplicationModuleListener(id = "tenancy.tenant-realm-ready.activation")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onTenantRealmReady(TenantRealmReady event) {
    TenantContextHolder.withTenantAndCorrelation(
        event.tenantId(),
        "tenant-realm-ready:" + event.eventId(),
        () -> {
          log.info("Activating tenant {} — schema + realm ready", event.tenantId());

          if (!provisioningRepository.claimActivation(event.tenantId())) {
            log.info(
                "Tenant {} is already active; ignoring duplicate activation", event.tenantId());
            return;
          }

          String schemaName = provisioningRepository.findSchemaName(event.tenantId());
          TenantActivated activated =
              new TenantActivated(
                  UUID.randomUUID(),
                  event.tenantId(),
                  event.slug(),
                  schemaName,
                  event.keycloakRealm());
          eventPublisher.publishEvent(activated);

          log.info(
              "Tenant {} activated. Schema={}, Realm={}",
              event.tenantId(),
              schemaName,
              event.keycloakRealm());
        });
  }
}
