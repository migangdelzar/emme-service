package com.emme.assistant.ai.adapter.out.tool;

import com.emme.appointments.api.command.RescheduleAppointmentCommand;
import com.emme.appointments.api.type.AppointmentActor;
import com.emme.appointments.api.usecase.RescheduleAuthorizedAppointmentUseCase;
import com.emme.assistant.ai.application.tool.AiToolExecutionContext;
import com.emme.assistant.ai.application.tool.AiToolHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class RescheduleAppointmentToolHandler implements AiToolHandler {
  private final RescheduleAuthorizedAppointmentUseCase u;
  private final ObjectMapper m;

  public RescheduleAppointmentToolHandler(
      RescheduleAuthorizedAppointmentUseCase u, ObjectMapper m) {
    this.u = u;
    this.m = m;
  }

  public String execute(AiToolExecutionContext c, Map<String, String> a) {
    UUID appointmentId;
    Instant startsAt;
    Instant endsAt;
    try {
      appointmentId = UUID.fromString(a.get("appointmentId"));
      startsAt = Instant.parse(a.get("startsAt"));
      endsAt = Instant.parse(a.get("endsAt"));
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid appointment arguments", e);
    }
    try {
      return m.writeValueAsString(
          u.reschedule(
              new RescheduleAppointmentCommand(
                  new AppointmentActor(
                      c.tenantId(), c.principalId(), c.roles(), c.idempotencyKey()),
                  appointmentId,
                  startsAt,
                  endsAt,
                  true)));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
