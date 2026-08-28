package com.emme.ai.adapter.out.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.tool.AiToolExecutionContext;
import com.emme.services.api.result.ServiceDetails;
import com.emme.services.api.usecase.ListActiveServiceCatalogEntriesUseCase;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServiceCatalogAiToolHandlerTest {

  @Test
  void readsTheAuthenticatedTenantCatalogAndReturnsDeterministicServiceInformation() {
    ListActiveServiceCatalogEntriesUseCase useCase =
        mock(ListActiveServiceCatalogEntriesUseCase.class);
    UUID tenantId = UUID.randomUUID();
    when(useCase.listActive(tenantId))
        .thenReturn(
            List.of(
                new ServiceDetails(
                    UUID.randomUUID(),
                    "gel",
                    "Gel manicure",
                    "MANICURE",
                    "Long-lasting finish",
                    60,
                    new BigDecimal("450.00"),
                    "ACTIVE")));
    ServiceCatalogAiToolHandler handler = new ServiceCatalogAiToolHandler(useCase);

    String response =
        handler.execute(context(tenantId), Map.of("tenantId", UUID.randomUUID().toString()));

    assertThat(response)
        .isEqualTo("Active salon services:\n- Gel manicure (60 min, base price 450.00)");
    verify(useCase).listActive(tenantId);
  }

  @Test
  void reportsWhenTheAuthenticatedTenantHasNoActiveServices() {
    ListActiveServiceCatalogEntriesUseCase useCase =
        mock(ListActiveServiceCatalogEntriesUseCase.class);
    UUID tenantId = UUID.randomUUID();
    when(useCase.listActive(tenantId)).thenReturn(List.of());
    ServiceCatalogAiToolHandler handler = new ServiceCatalogAiToolHandler(useCase);

    assertThat(handler.execute(context(tenantId), Map.of()))
        .isEqualTo("No active salon services are currently available.");
  }

  private static AiToolExecutionContext context(UUID tenantId) {
    UUID id = UUID.randomUUID();
    return new AiToolExecutionContext(
        tenantId, UUID.randomUUID(), Set.of("client"), id, id, "trace-" + id, "idem-" + id);
  }
}
