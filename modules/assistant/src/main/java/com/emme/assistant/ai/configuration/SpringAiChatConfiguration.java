package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.assistant.ai.adapter.out.provider.springai.SpringAiToolCallbackProvider;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.InputGuardAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.OutputGuardAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.PromptVersionAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.TenantSecurityAdvisor;
import com.emme.assistant.ai.application.port.out.IdentifiedChatCompletionPort;
import com.emme.assistant.ai.application.provider.ChatModelSelector;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientBuilderConfigurer;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Opt-in Spring AI chat provider registry and ordered fallback chain. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SpringAiChatProperties.class)
@ConditionalOnProperty(prefix = "app.ai.spring-chat", name = "enabled", havingValue = "true")
public class SpringAiChatConfiguration {

  @Bean(name = "ollamaChatModel")
  @ConditionalOnMissingBean(name = "ollamaChatModel")
  ChatModel ollamaChatModel(
      AiProviderProperties aiProperties, ObservationRegistry observationRegistry) {
    return OllamaChatModel.builder()
        .ollamaApi(OllamaApi.builder().baseUrl(aiProperties.chat().baseUrl()).build())
        .options(OllamaChatOptions.builder().model(aiProperties.chat().model()).build())
        .observationRegistry(observationRegistry)
        .build();
  }

  @Bean(name = "ollamaChatClient")
  @ConditionalOnMissingBean(name = "ollamaChatClient")
  ChatClient ollamaChatClient(
      ChatModel ollamaChatModel,
      ObservationRegistry observationRegistry,
      ChatClientBuilderConfigurer builderConfigurer) {
    return builderConfigurer
        .configure(ChatClient.builder(ollamaChatModel, observationRegistry, null, null))
        .build();
  }

  @Bean
  SpringAiChatProviderRegistry chatProviderRegistry(
      Map<String, ChatClient> chatClients,
      SpringAiChatProperties properties,
      TenantSecurityAdvisor tenantSecurityAdvisor,
      PromptVersionAdvisor promptVersionAdvisor,
      Optional<InputGuardAdvisor> inputGuardAdvisor,
      Optional<OutputGuardAdvisor> outputGuardAdvisor,
      AiTraceRecorder traceRecorder,
      Optional<SpringAiToolCallbackProvider> toolCallbackProvider,
      Optional<ToolSearchToolCallingAdvisor> toolSearchAdvisor) {
    List<Advisor> configuredAdvisors =
        new ArrayList<>(List.of(tenantSecurityAdvisor, promptVersionAdvisor));
    inputGuardAdvisor.ifPresent(configuredAdvisors::add);
    outputGuardAdvisor.ifPresent(configuredAdvisors::add);
    if (toolCallbackProvider.isPresent() && toolSearchAdvisor.isPresent()) {
      configuredAdvisors.add(toolSearchAdvisor.orElseThrow());
    }
    configuredAdvisors = SpringAiAdvisorConfiguration.orderedAdvisors(configuredAdvisors);
    return new SpringAiChatProviderRegistry(
        chatClients,
        properties,
        configuredAdvisors,
        traceRecorder,
        toolCallbackProvider.orElse(null));
  }

  @Bean(name = "aiChatCompletion")
  @ConditionalOnMissingBean(name = "aiChatCompletion")
  IdentifiedChatCompletionPort chatCompletionPort(
      SpringAiChatProviderRegistry registry,
      Optional<ModelExecutionScheduler> scheduler,
      AiExecutorProperties executionProperties) {
    if (scheduler.isEmpty()) {
      return new ChatModelSelector(registry.providers());
    }
    return new ChatModelSelector(
        registry.providers(), scheduler.orElseThrow(), executionProperties.modelAdmissionTimeout());
  }
}
