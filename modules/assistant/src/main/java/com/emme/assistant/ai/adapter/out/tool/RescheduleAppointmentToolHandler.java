package com.emme.assistant.ai.adapter.out.tool;

import com.emme.appointments.api.command.*;
import com.emme.appointments.api.usecase.*;
import com.emme.assistant.ai.application.tool.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;

public final class RescheduleAppointmentToolHandler implements AiToolHandler {
  private final RescheduleAuthorizedAppointmentUseCase u;
  private final ObjectMapper m;

  public RescheduleAppointmentToolHandler(
      RescheduleAuthorizedAppointmentUseCase u, ObjectMapper m) {
    this.u = u;
    this.m = m;
  }

  public String execute(AiToolExecutionContext c, Map<String, String> a) {
    try {
      return m.writeValueAsString(
          u.reschedule(
              new RescheduleAppointmentCommand(
                  new AppointmentActor(
                      c.tenantId(), c.principalId(), c.roles(), c.idempotencyKey()),
                  UUID.fromString(a.get("appointmentId")),
                  Instant.parse(a.get("startsAt")),
                  Instant.parse(a.get("endsAt")),
                  true)));
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid appointment arguments", e);
    }
  }
}
