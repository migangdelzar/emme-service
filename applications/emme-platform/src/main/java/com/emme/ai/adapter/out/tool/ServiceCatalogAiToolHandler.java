package com.emme.ai.adapter.out.tool;

import com.emme.assistant.ai.application.tool.AiToolExecutionContext;
import com.emme.assistant.ai.application.tool.AiToolHandler;
import com.emme.services.api.result.ServiceDetails;
import com.emme.services.api.usecase.ListActiveServiceCatalogEntriesUseCase;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Adapts the authoritative service-catalog use case to a read-only AI tool. */
public final class ServiceCatalogAiToolHandler implements AiToolHandler {

  private final ListActiveServiceCatalogEntriesUseCase listServices;

  public ServiceCatalogAiToolHandler(ListActiveServiceCatalogEntriesUseCase listServices) {
    this.listServices = Objects.requireNonNull(listServices, "listServices must not be null");
  }

  @Override
  public String execute(AiToolExecutionContext context, Map<String, String> arguments) {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(arguments, "arguments must not be null");
    var services = listServices.listActive(context.tenantId());
    if (services.isEmpty()) {
      return "No active salon services are currently available.";
    }
    return services.stream()
        .map(ServiceCatalogAiToolHandler::format)
        .collect(Collectors.joining("\n", "Active salon services:\n", ""));
  }

  private static String format(ServiceDetails service) {
    String price =
        service.basePrice() == null ? "available on request" : service.basePrice().toPlainString();
    return "- %s (%d min, base price %s)"
        .formatted(service.name(), service.durationMinutes(), price);
  }
}
