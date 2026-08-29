package com.emme.assistant.ai.adapter.out.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.appointments.api.result.AvailableSlot;
import com.emme.appointments.api.usecase.FindAvailableSlotsUseCase;
import com.emme.assistant.ai.application.tool.AiToolExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AvailabilityToolHandlerTest {

  @Test
  void delegatesAvailabilityToTheUseCaseWithParsedArgumentsAndBackendTenant() {
    UUID tenantId = UUID.randomUUID();
    UUID serviceId = UUID.randomUUID();
    FindAvailableSlotsUseCase findAvailability = mock();
    when(findAvailability.find(tenantId, serviceId, LocalDate.of(2026, 8, 29)))
        .thenReturn(
            List.of(
                new AvailableSlot(
                    UUID.randomUUID(),
                    Instant.parse("2026-08-29T17:00:00Z"),
                    Instant.parse("2026-08-29T18:00:00Z"))));
    AvailabilityToolHandler handler =
        new AvailabilityToolHandler(
            findAvailability, new ObjectMapper().registerModule(new JavaTimeModule()));

    String result =
        handler.execute(
            new AiToolExecutionContext(
                tenantId,
                UUID.randomUUID(),
                Set.of("client"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "trace-availability",
                "idem-availability"),
            Map.of("serviceId", serviceId.toString(), "date", "2026-08-29"));

    assertThat(result).contains("artistId").contains("1788022800");
    verify(findAvailability).find(tenantId, serviceId, LocalDate.of(2026, 8, 29));
  }
}
