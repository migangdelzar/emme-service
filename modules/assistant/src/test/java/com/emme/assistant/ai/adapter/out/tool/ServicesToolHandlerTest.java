package com.emme.assistant.ai.adapter.out.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.tool.AiToolExecutionContext;
import com.emme.services.api.result.ServiceDetails;
import com.emme.services.api.usecase.ListActiveServiceCatalogEntriesUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServicesToolHandlerTest {

  @Test
  void delegatesServiceListingToTheUseCaseWithTheBackendTenant() {
    UUID tenantId = UUID.randomUUID();
    ListActiveServiceCatalogEntriesUseCase listServices = mock();
    when(listServices.listActive(tenantId))
        .thenReturn(
            List.of(
                new ServiceDetails(
                    UUID.randomUUID(),
                    "gel",
                    "Gel manicure",
                    "MANICURE",
                    "Long-lasting finish",
                    60,
                    BigDecimal.valueOf(450),
                    "ACTIVE")));
    ServicesToolHandler handler = new ServicesToolHandler(listServices, new ObjectMapper());

    String result =
        handler.execute(
            new AiToolExecutionContext(
                tenantId,
                UUID.randomUUID(),
                Set.of("client"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "trace-services",
                "idem-services"),
            Map.of());

    assertThat(result).contains("Gel manicure").contains("450").contains("60");
    verify(listServices).listActive(tenantId);
  }
}
