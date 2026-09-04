package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.adapter.out.tool.ServicesToolHandler;
import com.emme.assistant.ai.application.tool.AiToolDefinition;
import com.emme.assistant.ai.application.tool.AiToolRisk;
import com.emme.kernel.context.Channel;
import com.emme.services.api.usecase.ListActiveServiceCatalogEntriesUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers the tenant-safe service catalog tool through the Services use case. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(ListActiveServiceCatalogEntriesUseCase.class)
public class ServicesToolConfiguration {

  @Bean
  AiToolDefinition getSalonServicesTool(
      ListActiveServiceCatalogEntriesUseCase listServices, ObjectMapper objectMapper) {
    return new AiToolDefinition(
        "getSalonServices",
        "List active salon services and their current catalog details",
        java.util.Set.of("client", "tenant_staff", "tenant_owner", "admin"),
        AiToolRisk.READ_ONLY,
        false,
        false,
        new ServicesToolHandler(listServices, objectMapper),
        java.util.Set.of(),
        java.util.Set.of("locale"),
        java.util.Set.of("service_catalog"),
        java.util.Set.of("ai_chat"),
        java.util.Set.of(Channel.WEB, Channel.WHATSAPP));
  }
}
