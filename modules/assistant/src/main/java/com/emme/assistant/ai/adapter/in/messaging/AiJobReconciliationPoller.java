package com.emme.assistant.ai.adapter.in.messaging;

import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import com.emme.assistant.ai.configuration.AiJobProperties;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextBridge;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.tenancy.application.port.out.TenantRepository;
import com.emme.tenancy.domain.model.TenantStatus;
import java.util.Set;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Reconciles durable jobs whose event delivery was lost. Redis/live events remain optional. */
@Component
public final class AiJobReconciliationPoller {
  private static final UUID SYSTEM_ID = new UUID(0, 0);

  private final AiJobStatusStore store;
  private final AiJobListener listener;
  private final AiJobProperties properties;
  private final TenantRepository tenants;

  public AiJobReconciliationPoller(
      AiJobStatusStore store,
      AiJobListener listener,
      AiJobProperties properties,
      TenantRepository tenants) {
    this.store = store;
    this.listener = listener;
    this.properties = properties;
    this.tenants = tenants;
  }

  @Scheduled(fixedDelayString = "${app.ai.jobs.reconciliation-delay-ms:5000}")
  public void reconcile() {
    tenants.findByStatus(TenantStatus.ACTIVE).stream()
        .map(tenant -> tenant.id())
        .forEach(this::reconcileTenant);
  }

  private void reconcileTenant(UUID tenantId) {
    AiExecutionContext context =
        new AiExecutionContext(
            tenantId,
            SYSTEM_ID,
            Set.of("SYSTEM"),
            SYSTEM_ID,
            SYSTEM_ID,
            "ai-job-reconciliation:" + tenantId,
            "ai-job-reconciliation:" + tenantId);
    AiExecutionContextScope.run(
        context,
        () ->
            AiExecutionContextBridge.runCurrent(
                () ->
                    store.claimAvailable(properties.pollLimit()).forEach(listener::onClaimedJob)));
  }
}
