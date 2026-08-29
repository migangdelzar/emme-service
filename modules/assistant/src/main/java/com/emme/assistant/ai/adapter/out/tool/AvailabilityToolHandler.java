package com.emme.assistant.ai.adapter.out.tool;

import com.emme.appointments.api.result.AvailableSlot;
import com.emme.appointments.api.usecase.FindAvailableSlotsUseCase;
import com.emme.assistant.ai.application.tool.AiToolExecutionContext;
import com.emme.assistant.ai.application.tool.AiToolHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Adapts deterministic availability lookup to the controlled AI tool boundary. */
public final class AvailabilityToolHandler implements AiToolHandler {

  private final FindAvailableSlotsUseCase findAvailability;
  private final ObjectMapper objectMapper;

  public AvailabilityToolHandler(
      FindAvailableSlotsUseCase findAvailability, ObjectMapper objectMapper) {
    this.findAvailability =
        Objects.requireNonNull(findAvailability, "findAvailability must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public String execute(AiToolExecutionContext context, Map<String, String> arguments) {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(arguments, "arguments must not be null");
    UUID serviceId = parseUuid(arguments.get("serviceId"), "serviceId");
    LocalDate date = parseDate(arguments.get("date"));
    List<AvailableSlot> slots = findAvailability.find(context.tenantId(), serviceId, date);
    try {
      return objectMapper.writeValueAsString(slots);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize available appointment slots", exception);
    }
  }

  private static UUID parseUuid(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(field + " must be a UUID", exception);
    }
  }

  private static LocalDate parseDate(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("date must not be blank");
    }
    try {
      return LocalDate.parse(value);
    } catch (java.time.format.DateTimeParseException exception) {
      throw new IllegalArgumentException("date must be ISO-8601", exception);
    }
  }
}
