package com.emme.assistant.ai.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import com.emme.assistant.ai.application.port.out.AiJobMetrics;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
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
import java.util.UUID;
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

  private static Tenant tenant(UUID id) {
    Instant now = Instant.parse("2026-08-31T00:00:00Z");
    return Tenant.rehydrate(
        id, "tenant-" + id, "Tenant " + id, TenantStatus.ACTIVE, null, "emme", now, now);
  }
}
