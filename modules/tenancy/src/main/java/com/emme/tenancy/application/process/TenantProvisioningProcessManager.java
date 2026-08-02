package com.emme.tenancy.application.process;

import com.emme.kernel.context.TenantContextHolder;
import com.emme.kernel.tracing.CorrelationId;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Coordinates the long-running tenant schema provisioning process. */
@Component
final class TenantProvisioningProcessManager {

  private static final Logger log = LoggerFactory.getLogger(TenantProvisioningProcessManager.class);

  private final TenantProvisioningRepository provisioningRepository;
  private final TenantSchemaMigrationPort schemaMigrationPort;

  TenantProvisioningProcessManager(
      TenantProvisioningRepository provisioningRepository,
      TenantSchemaMigrationPort schemaMigrationPort) {
    this.provisioningRepository = provisioningRepository;
    this.schemaMigrationPort = schemaMigrationPort;
  }

  @Scheduled(fixedDelayString = "${app.tenant.provisioning.poll-interval:PT10S}")
  @SchedulerLock(name = "tenant-provisioning", lockAtMostFor = "PT5M")
  public void processProvisioningRequests() {
    List<TenantProvisioningRepository.TenantProvisioningRequest> pending;
    try {
      pending = provisioningRepository.findPending();
    } catch (Exception exception) {
      log.error("Unable to load pending tenant provisioning requests: {}", boundedError(exception));
      return;
    }

    if (pending.isEmpty()) return;

    log.info("Found {} tenants awaiting provisioning", pending.size());

    for (TenantProvisioningRepository.TenantProvisioningRequest row : pending) {
      TenantContextHolder.withTenantAndCorrelation(
          row.tenantId(),
          CorrelationId.generate(),
          () -> {
            try {
              schemaMigrationPort.migrate(row.schemaName());
              provisioningRepository.markActive(row.tenantId());
              log.info(
                  "Tenant {} (schema: {}) provisioned successfully", row.slug(), row.schemaName());
            } catch (Exception e) {
              String error = boundedError(e);
              log.error("Failed to provision tenant {}: {}", row.slug(), error);
              provisioningRepository.markFailed(row.tenantId(), error);
            }
            return null;
          });
    }
  }

  private String boundedError(Exception exception) {
    String message = exception.getMessage();
    return message != null
        ? message.substring(0, Math.min(message.length(), 500))
        : "Unknown error";
  }
}
