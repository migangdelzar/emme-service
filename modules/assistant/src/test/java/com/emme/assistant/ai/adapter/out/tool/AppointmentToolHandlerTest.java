package com.emme.assistant.ai.adapter.out.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.appointments.api.command.CreateAppointmentCommand;
import com.emme.appointments.api.usecase.BookAppointmentUseCase;
import com.emme.appointments.api.usecase.CancelAuthorizedAppointmentUseCase;
import com.emme.appointments.api.usecase.RescheduleAuthorizedAppointmentUseCase;
import com.emme.assistant.ai.application.tool.AiToolExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AppointmentToolHandlerTest {
  private static final UUID TENANT = UUID.randomUUID();
  private static final UUID PRINCIPAL = UUID.randomUUID();
  private static final AiToolExecutionContext CONTEXT =
      new AiToolExecutionContext(
          TENANT,
          PRINCIPAL,
          Set.of("client"),
          UUID.randomUUID(),
          UUID.randomUUID(),
          "trace",
          "key");

  @Test
  void createRejectsMalformedArgumentsBeforeCallingTheUseCase() {
    BookAppointmentUseCase useCase = mock(BookAppointmentUseCase.class);
    CreateAppointmentToolHandler handler =
        new CreateAppointmentToolHandler(useCase, new ObjectMapper());

    assertThatThrownBy(() -> handler.execute(CONTEXT, Map.of("customerId", "not-a-uuid")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid appointment arguments");
    verifyNoInteractions(useCase);
  }

  @Test
  void createPropagatesBackendContextIntoTheDomainCommand() {
    BookAppointmentUseCase useCase = mock(BookAppointmentUseCase.class);
    when(useCase.book(any()))
        .thenReturn(mock(com.emme.appointments.api.result.AppointmentDetails.class));
    CreateAppointmentToolHandler handler =
        new CreateAppointmentToolHandler(useCase, new ObjectMapper());
    UUID customer = UUID.randomUUID();
    UUID service = UUID.randomUUID();
    UUID artist = UUID.randomUUID();

    handler.execute(
        CONTEXT,
        Map.of(
            "customerId",
            customer.toString(),
            "serviceId",
            service.toString(),
            "artistId",
            artist.toString(),
            "startsAt",
            "2030-01-01T10:00:00Z",
            "endsAt",
            "2030-01-01T11:00:00Z"));

    ArgumentCaptor<CreateAppointmentCommand> captor =
        ArgumentCaptor.forClass(CreateAppointmentCommand.class);
    verify(useCase).book(captor.capture());
    assertThat(captor.getValue().actor().tenantId()).isEqualTo(TENANT);
    assertThat(captor.getValue().actor().principalId()).isEqualTo(PRINCIPAL);
    assertThat(captor.getValue().actor().idempotencyKey()).isEqualTo("key");
  }

  @Test
  void createPreservesDomainSecurityAndCollisionExceptions() {
    RuntimeException failure = new CollisionFailure("collision");
    BookAppointmentUseCase useCase = mock(BookAppointmentUseCase.class);
    when(useCase.book(any())).thenThrow(failure);
    CreateAppointmentToolHandler handler =
        new CreateAppointmentToolHandler(useCase, new ObjectMapper());
    Map<String, String> arguments =
        Map.of(
            "customerId",
            UUID.randomUUID().toString(),
            "serviceId",
            UUID.randomUUID().toString(),
            "artistId",
            UUID.randomUUID().toString(),
            "startsAt",
            "2030-01-01T10:00:00Z",
            "endsAt",
            "2030-01-01T11:00:00Z");

    assertThatThrownBy(() -> handler.execute(CONTEXT, arguments)).isSameAs(failure);
  }

  @Test
  void cancelPreservesSecurityException() {
    RuntimeException failure = new SecurityException("forbidden");
    CancelAuthorizedAppointmentUseCase useCase = mock(CancelAuthorizedAppointmentUseCase.class);
    when(useCase.cancel(any())).thenThrow(failure);
    CancelAppointmentToolHandler handler =
        new CancelAppointmentToolHandler(useCase, new ObjectMapper());

    assertThatThrownBy(
            () -> handler.execute(CONTEXT, Map.of("appointmentId", UUID.randomUUID().toString())))
        .isSameAs(failure);
  }

  @Test
  void rescheduleRejectsMalformedArgumentsAndPreservesDomainFailure() {
    RescheduleAuthorizedAppointmentUseCase useCase =
        mock(RescheduleAuthorizedAppointmentUseCase.class);
    RescheduleAppointmentToolHandler handler =
        new RescheduleAppointmentToolHandler(useCase, new ObjectMapper());
    assertThatThrownBy(() -> handler.execute(CONTEXT, Map.of("appointmentId", "bad")))
        .isInstanceOf(IllegalArgumentException.class);
    RuntimeException failure = new IllegalStateException("not confirmed");
    when(useCase.reschedule(any())).thenThrow(failure);
    assertThatThrownBy(
            () ->
                handler.execute(
                    CONTEXT,
                    Map.of(
                        "appointmentId",
                        UUID.randomUUID().toString(),
                        "startsAt",
                        "2030-01-01T10:00:00Z",
                        "endsAt",
                        "2030-01-01T11:00:00Z")))
        .isSameAs(failure);
  }

  private static final class CollisionFailure extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private CollisionFailure(String message) {
      super(message);
    }
  }
}
