package com.emme.configuration;

import com.emme.ai.adapter.out.tool.ServiceCatalogAiToolHandler;
import com.emme.assistant.ai.application.tool.AiToolDefinition;
import com.emme.assistant.ai.application.tool.AiToolRisk;
import com.emme.kernel.context.Channel;
import com.emme.services.api.usecase.ListActiveServiceCatalogEntriesUseCase;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers platform-owned AI tools while keeping domain rules in module use cases. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(ListActiveServiceCatalogEntriesUseCase.class)
public class SpringAiPlatformToolConfiguration {

  @Bean
  AiToolDefinition getSalonServicesTool(
      ListActiveServiceCatalogEntriesUseCase listActiveServiceCatalogEntries) {
    return new AiToolDefinition(
        "getSalonServices",
        "List active salon services and their authoritative duration and base price",
        Set.of("admin", "tenant_owner", "tenant_staff", "client"),
        AiToolRisk.READ_ONLY,
        false,
        false,
        new ServiceCatalogAiToolHandler(listActiveServiceCatalogEntries),
        Set.of("service_catalog"),
        Set.of("ai_chat"),
        Set.of(Channel.WEB, Channel.WHATSAPP));
  }
}
