package com.emme.assistant.ai.adapter.in.messaging;

import com.emme.assistant.ai.application.port.out.AiJobMetrics;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import com.emme.assistant.ai.configuration.AiJobProperties;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextBridge;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.tenancy.api.query.ListActiveTenantsQuery;
import com.emme.tenancy.api.usecase.ListActiveTenantsUseCase;
import java.util.Comparator;
import java.util.List;
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
  private final ListActiveTenantsUseCase tenants;
  private final AiJobMetrics metrics;
  private UUID nextTenantId;

  public AiJobReconciliationPoller(
      AiJobStatusStore store,
      AiJobListener listener,
      AiJobProperties properties,
      ListActiveTenantsUseCase tenants,
      AiJobMetrics metrics) {
    this.store = store;
    this.listener = listener;
    this.properties = properties;
    this.tenants = tenants;
    this.metrics = metrics;
  }

  @Scheduled(fixedDelayString = "${app.ai.jobs.reconciliation-delay-ms:5000}")
  public synchronized void reconcile() {
    List<UUID> activeTenantIds =
        tenants.list(new ListActiveTenantsQuery()).stream()
            .map(tenant -> tenant.id())
            .distinct()
            .sorted(Comparator.naturalOrder())
            .toList();
    if (activeTenantIds.isEmpty()) {
      nextTenantId = null;
      return;
    }

    int start = startingIndex(activeTenantIds);
    int claimBudget = properties.pollLimit();
    for (int offset = 0; offset < claimBudget; offset++) {
      reconcileTenant(activeTenantIds.get((start + offset) % activeTenantIds.size()));
    }
    nextTenantId = activeTenantIds.get((start + claimBudget) % activeTenantIds.size());
  }

  private int startingIndex(List<UUID> activeTenantIds) {
    if (nextTenantId == null) return 0;
    int exactIndex = activeTenantIds.indexOf(nextTenantId);
    if (exactIndex >= 0) return exactIndex;
    for (int index = 0; index < activeTenantIds.size(); index++) {
      if (activeTenantIds.get(index).compareTo(nextTenantId) > 0) return index;
    }
    return 0;
  }

  private void reconcileTenant(UUID tenantId) {
    metrics.recordTenantFairness();
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
                () -> store.claimAvailable(1).forEach(listener::onClaimedJob)));
  }
}
