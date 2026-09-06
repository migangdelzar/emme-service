package com.emme.assistant.ai.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.job.AiJobRequest;
import com.emme.ai.contracts.job.AiJobType;
import com.emme.assistant.ai.application.job.AiJobWorker;
import com.emme.assistant.ai.application.port.out.AiJobMetrics;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import com.emme.assistant.ai.application.port.out.NoopAiJobMetrics;
import com.emme.assistant.ai.configuration.AiJobProperties;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.tenancy.api.query.ListActiveTenantsQuery;
import com.emme.tenancy.api.result.TenantDetails;
import com.emme.tenancy.api.usecase.ListActiveTenantsUseCase;
import com.emme.tenancy.api.type.TenantStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;

class AiJobReconciliationPollerTest {

  private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TENANT_B = UUID.fromString("00000000-0000-0000-0000-000000000002");

  @Test
  void claimsAvailableJobsInsideTheAuthoritativeTenantAiContext() {
    UUID tenantA = TENANT_A;
    UUID tenantB = TENANT_B;
    ListActiveTenantsUseCase tenants = mock(ListActiveTenantsUseCase.class);
    when(tenants.list(new ListActiveTenantsQuery()))
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
            store, mock(AiJobListener.class), new AiJobProperties(1, 1, 3, 2), tenants, metrics);

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
    UUID tenantA = TENANT_A;
    UUID tenantB = TENANT_B;
    AiExecutionContext contextA = context(tenantA, "a");
    AiExecutionContext contextB = context(tenantB, "b");
    AiJobRequest requestA = request(contextA);
    AiJobRequest requestB = request(contextB);
    ListActiveTenantsUseCase tenants = mock(ListActiveTenantsUseCase.class);
    when(tenants.list(new ListActiveTenantsQuery()))
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
        new AiJobListener(mock(AiJobWorker.class), executor, store, NoopAiJobMetrics.INSTANCE);

    new AiJobReconciliationPoller(
            store, listener, new AiJobProperties(1, 1, 3, 2), tenants, NoopAiJobMetrics.INSTANCE)
        .reconcile();

    assertThat(events)
        .containsExactly(
            "claim:" + tenantA, "defer:" + tenantA, "claim:" + tenantB, "defer:" + tenantB);
  }

  @Test
  void rotatesDeterministicallyAcrossSaturatedReconciliationCycles() {
    ListActiveTenantsUseCase tenants = mock(ListActiveTenantsUseCase.class);
    when(tenants.list(new ListActiveTenantsQuery()))
        .thenReturn(List.of(tenant(TENANT_B), tenant(TENANT_A)));
    AiJobStatusStore store = mock(AiJobStatusStore.class);
    List<UUID> claimedTenants = new ArrayList<>();
    when(store.claimAvailable(1))
        .thenAnswer(
            invocation -> {
              UUID tenantId = AiExecutionContextScope.requireCurrent().tenantId();
              claimedTenants.add(tenantId);
              return List.of(request(context(tenantId, tenantId.toString())));
            });
    doNothing().when(store).defer(any(), any(), any());
    ExecutorService saturatedExecutor = mock(ExecutorService.class);
    doThrow(new RejectedExecutionException()).when(saturatedExecutor).execute(any());
    AiJobListener listener =
        new AiJobListener(
            mock(AiJobWorker.class), saturatedExecutor, store, NoopAiJobMetrics.INSTANCE);
    AiJobReconciliationPoller poller =
        new AiJobReconciliationPoller(
            store, listener, new AiJobProperties(1, 1, 3, 1), tenants, NoopAiJobMetrics.INSTANCE);

    poller.reconcile();
    poller.reconcile();
    poller.reconcile();

    assertThat(claimedTenants).containsExactly(TENANT_A, TENANT_B, TENANT_A);
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

  private static TenantDetails tenant(UUID id) {
    return new TenantDetails(
        id, "tenant-" + id, "Tenant " + id, null, TenantStatus.ACTIVE, null, "emme");
  }
}
