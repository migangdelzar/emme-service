package com.emme.tenancy.adapter.in.messaging.consumer;

import com.emme.tenancy.api.event.TenantCreated;
import com.emme.tenancy.api.event.TenantSchemaReady;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Listens for {@link TenantCreated} events and provisions tenant schemas via Liquibase. */
@Component
public class TenantSchemaProvisioningListener {

  private static final Logger log =
      LoggerFactory.getLogger(TenantSchemaProvisioningListener.class);

  private final TenantSchemaMigrationPort schemaMigrationPort;
  private final TenantProvisioningRepository provisioningRepository;
  private final ApplicationEventPublisher eventPublisher;

  public TenantSchemaProvisioningListener(
      TenantSchemaMigrationPort schemaMigrationPort,
      TenantProvisioningRepository provisioningRepository,
      ApplicationEventPublisher eventPublisher) {
    this.schemaMigrationPort = schemaMigrationPort;
    this.provisioningRepository = provisioningRepository;
    this.eventPublisher = eventPublisher;
  }

  @ApplicationModuleListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onTenantCreated(TenantCreated event) {
    log.info("Provisioning schema for tenant {} (slug={})", event.tenantId(), event.slug());

    try {
      String schemaName = schemaMigrationPort.migrate(event.tenantId(), event.slug());
      log.info("Schema {} provisioned for tenant {}", schemaName, event.tenantId());

      TenantSchemaReady ready =
          new TenantSchemaReady(
              UUID.randomUUID(), event.tenantId(), event.slug(), schemaName);
      eventPublisher.publishEvent(ready);
    } catch (Exception e) {
      log.error(
          "Schema provisioning failed for tenant {}: {}", event.tenantId(), e.getMessage());
      provisioningRepository.markFailed(event.tenantId(), e.getMessage());
      throw e;
    }
  }
}
