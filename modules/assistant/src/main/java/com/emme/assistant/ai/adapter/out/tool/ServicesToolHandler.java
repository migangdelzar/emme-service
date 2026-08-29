package com.emme.assistant.ai.adapter.out.tool;

import com.emme.assistant.ai.application.tool.AiToolExecutionContext;
import com.emme.assistant.ai.application.tool.AiToolHandler;
import com.emme.services.api.result.ServiceDetails;
import com.emme.services.api.usecase.ListActiveServiceCatalogEntriesUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Adapts the authoritative Services use case to the controlled AI tool boundary. */
public final class ServicesToolHandler implements AiToolHandler {

  private final ListActiveServiceCatalogEntriesUseCase listServices;
  private final ObjectMapper objectMapper;

  public ServicesToolHandler(
      ListActiveServiceCatalogEntriesUseCase listServices, ObjectMapper objectMapper) {
    this.listServices = Objects.requireNonNull(listServices, "listServices must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public String execute(AiToolExecutionContext context, Map<String, String> arguments) {
    Objects.requireNonNull(context, "context must not be null");
    List<ServiceDetails> services = listServices.listActive(context.tenantId());
    try {
      return objectMapper.writeValueAsString(services);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize salon services", exception);
    }
  }
}
