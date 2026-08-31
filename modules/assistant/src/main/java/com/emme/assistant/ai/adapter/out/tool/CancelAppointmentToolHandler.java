package com.emme.assistant.ai.adapter.out.tool;

import com.emme.appointments.api.command.*;
import com.emme.appointments.api.usecase.*;
import com.emme.assistant.ai.application.tool.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public final class CancelAppointmentToolHandler implements AiToolHandler {
  private final CancelAuthorizedAppointmentUseCase u;
  private final ObjectMapper m;

  public CancelAppointmentToolHandler(CancelAuthorizedAppointmentUseCase u, ObjectMapper m) {
    this.u = u;
    this.m = m;
  }

  public String execute(AiToolExecutionContext c, Map<String, String> a) {
    UUID appointmentId;
    try {
      appointmentId = UUID.fromString(a.get("appointmentId"));
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid appointment arguments", e);
    }
    try {
      return m.writeValueAsString(
          u.cancel(
              new CancelAppointmentCommand(
                  new AppointmentActor(
                      c.tenantId(), c.principalId(), c.roles(), c.idempotencyKey()),
                  appointmentId,
                  true)));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
