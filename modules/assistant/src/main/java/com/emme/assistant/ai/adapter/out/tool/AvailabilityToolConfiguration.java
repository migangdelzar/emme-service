package com.emme.assistant.ai.adapter.out.tool;

import com.emme.appointments.api.usecase.FindAvailableSlotsUseCase;
import com.emme.assistant.ai.application.tool.AiToolDefinition;
import com.emme.assistant.ai.application.tool.AiToolRisk;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers deterministic tenant-scoped availability lookup as a read-only AI tool. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(FindAvailableSlotsUseCase.class)
public class AvailabilityToolConfiguration {

  @Bean
  AiToolDefinition findAvailabilityTool(
      FindAvailableSlotsUseCase findAvailability, ObjectMapper objectMapper) {
    return new AiToolDefinition(
        "findAvailability",
        "Find available appointment slots for a service and date",
        java.util.Set.of("client", "tenant_staff", "tenant_owner", "admin"),
        AiToolRisk.READ_ONLY,
        false,
        false,
        new AvailabilityToolHandler(findAvailability, objectMapper),
        java.util.Set.of("serviceId", "date"),
        java.util.Set.of("serviceId", "date"));
  }
}
