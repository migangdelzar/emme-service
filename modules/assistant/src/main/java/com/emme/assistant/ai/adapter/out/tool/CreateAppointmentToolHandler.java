package com.emme.assistant.ai.adapter.out.tool;

import com.emme.appointments.api.command.*;
import com.emme.appointments.api.usecase.*;
import com.emme.assistant.ai.application.tool.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;

public final class CreateAppointmentToolHandler implements AiToolHandler {
  private final BookAppointmentUseCase u;
  private final ObjectMapper m;

  public CreateAppointmentToolHandler(BookAppointmentUseCase u, ObjectMapper m) {
    this.u = Objects.requireNonNull(u);
    this.m = Objects.requireNonNull(m);
  }

  public String execute(AiToolExecutionContext c, Map<String, String> a) {
    try {
      return m.writeValueAsString(
          u.book(
              new CreateAppointmentCommand(
                  new AppointmentActor(
                      c.tenantId(), c.principalId(), c.roles(), c.idempotencyKey()),
                  UUID.fromString(a.get("customerId")),
                  UUID.fromString(a.get("serviceId")),
                  UUID.fromString(a.get("artistId")),
                  Instant.parse(a.get("startsAt")),
                  Instant.parse(a.get("endsAt")),
                  true)));
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid appointment arguments", e);
    }
  }
}
