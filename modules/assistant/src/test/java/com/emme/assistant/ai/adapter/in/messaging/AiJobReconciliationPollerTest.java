package com.emme.assistant.ai.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import com.emme.ai.contracts.job.AiJobRequest;
import com.emme.ai.contracts.job.AiJobType;
import com.emme.assistant.ai.application.port.out.AiJobMetrics;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import com.emme.assistant.ai.application.port.out.NoopAiJobMetrics;
import com.emme.assistant.ai.application.service.AiJobWorkerService;
import com.emme.assistant.ai.configuration.AiJobProperties;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.tenancy.application.port.out.TenantRepository;
import com.emme.tenancy.domain.model.Tenant;
import com.emme.tenancy.domain.model.TenantStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;

class AiJobReconciliationPollerTest {

  @Test
  void claimsAvailableJobsInsideTheAuthoritativeTenantAiContext() {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    TenantRepository tenants = mock(TenantRepository.class);
    when(tenants.findByStatus(TenantStatus.ACTIVE))
        .thenReturn(List.of(tenant(tenantA), tenant(tenantB)));
    AiJobStatusStore store = mock(AiJobStatusStore.class);
    AiJobMetrics metrics = mock(AiJobMetrics.class);
    List<AiExecutionContext> observedContexts = new ArrayList<>();
    doAnswer(
            invocation -> {
              observedContexts.add(AiExecutionContextScope.requireCurrent());
              assertThat(TenantContextHolder.requireCurrentTenantId())
                  .isEqualTo(AiExecutionContextScope.requireCurrent().tenantId());
              return List.of();
            })
        .when(store)
        .claimAvailable(anyInt());

    AiJobReconciliationPoller poller =
        new AiJobReconciliationPoller(
            store, mock(AiJobListener.class), new AiJobProperties(1, 1, 3, 7), tenants, metrics);

    poller.reconcile();

    assertThat(observedContexts)
        .extracting(AiExecutionContext::tenantId)
        .containsExactly(tenantA, tenantB);
    verify(metrics, times(2)).recordTenantFairness();
    assertThat(TenantContextHolder.currentTenantOptional()).isEmpty();
    assertThat(AiExecutionContextScope.current()).isEmpty();
  }

  @Test
  void alternatesTenantsAndDefersEveryRejectedClaimImmediately() {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    AiExecutionContext contextA = context(tenantA, "a");
    AiExecutionContext contextB = context(tenantB, "b");
    AiJobRequest requestA = request(contextA);
    AiJobRequest requestB = request(contextB);
    TenantRepository tenants = mock(TenantRepository.class);
    when(tenants.findByStatus(TenantStatus.ACTIVE))
        .thenReturn(List.of(tenant(tenantA), tenant(tenantB)));
    AiJobStatusStore store = mock(AiJobStatusStore.class);
    List<String> events = new ArrayList<>();
    when(store.claimAvailable(1))
        .thenAnswer(
            invocation -> {
              UUID currentTenant = AiExecutionContextScope.requireCurrent().tenantId();
              events.add("claim:" + currentTenant);
              return List.of(currentTenant.equals(tenantA) ? requestA : requestB);
            });
    doAnswer(
            invocation -> {
              AiExecutionContext context = invocation.getArgument(1);
              events.add("defer:" + context.tenantId());
              return null;
            })
        .when(store)
        .defer(any(), any(), any());
    ExecutorService executor = mock(ExecutorService.class);
    doThrow(new RejectedExecutionException()).when(executor).execute(any());
    AiJobListener listener =
        new AiJobListener(
            mock(AiJobWorkerService.class), executor, store, NoopAiJobMetrics.INSTANCE);

    new AiJobReconciliationPoller(
            store, listener, new AiJobProperties(1, 1, 3, 1), tenants, NoopAiJobMetrics.INSTANCE)
        .reconcile();

    assertThat(events)
        .containsExactly(
            "claim:" + tenantA, "defer:" + tenantA, "claim:" + tenantB, "defer:" + tenantB);
  }

  private static AiExecutionContext context(UUID tenantId, String key) {
    return new AiExecutionContext(
        tenantId,
        UUID.randomUUID(),
        Set.of("SYSTEM"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-" + key,
        "idempotency-" + key);
  }

  private static AiJobRequest request(AiExecutionContext context) {
    return new AiJobRequest(UUID.randomUUID(), AiJobType.GRAPH_PROJECTION, "payload", context);
  }

  private static Tenant tenant(UUID id) {
    Instant now = Instant.parse("2026-08-31T00:00:00Z");
    return Tenant.rehydrate(
        id, "tenant-" + id, "Tenant " + id, TenantStatus.ACTIVE, null, "emme", now, now);
  }
}
