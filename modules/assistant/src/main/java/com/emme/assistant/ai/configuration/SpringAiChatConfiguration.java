package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.adapter.out.provider.springai.SpringAiToolCallbackProvider;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.PromptVersionAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.TenantSecurityAdvisor;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.provider.ChatModelSelector;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
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

  ChatModel ollamaChatModel(AiProperties aiProperties) {
    return ollamaChatModel(aiProperties, ObservationRegistry.NOOP);
  }

  @Bean(name = "ollamaChatModel")
  @ConditionalOnMissingBean(name = "ollamaChatModel")
  ChatModel ollamaChatModel(
      AiProperties aiProperties, ObservationRegistry observationRegistry) {
    return OllamaChatModel.builder()
        .ollamaApi(OllamaApi.builder().baseUrl(aiProperties.chat().baseUrl()).build())
        .options(OllamaChatOptions.builder().model(aiProperties.chat().model()).build())
        .observationRegistry(observationRegistry)
        .build();
  }

  ChatClient ollamaChatClient(ChatModel ollamaChatModel) {
    return ollamaChatClient(ollamaChatModel, ObservationRegistry.NOOP);
  }

  @Bean(name = "ollamaChatClient")
  @ConditionalOnMissingBean(name = "ollamaChatClient")
  ChatClient ollamaChatClient(
      ChatModel ollamaChatModel, ObservationRegistry observationRegistry) {
    return ChatClient.create(ollamaChatModel, observationRegistry);
  }

  @Bean
  SpringAiChatProviderRegistry chatProviderRegistry(
      Map<String, ChatClient> chatClients,
      SpringAiChatProperties properties,
      TenantSecurityAdvisor tenantSecurityAdvisor,
      PromptVersionAdvisor promptVersionAdvisor,
      AiTraceRecorder traceRecorder,
      Optional<SpringAiToolCallbackProvider> toolCallbackProvider,
      Optional<ToolSearchToolCallingAdvisor> toolSearchAdvisor) {
    List<Advisor> configuredAdvisors =
        new ArrayList<>(List.of(tenantSecurityAdvisor, promptVersionAdvisor));
    if (toolCallbackProvider.isPresent()) {
      toolSearchAdvisor.ifPresent(configuredAdvisors::add);
      return chatProviderRegistry(
          chatClients, properties, configuredAdvisors, traceRecorder, toolCallbackProvider.get());
    }
    return chatProviderRegistry(chatClients, properties, configuredAdvisors, traceRecorder);
  }

  SpringAiChatProviderRegistry chatProviderRegistry(
      Map<String, ChatClient> chatClients,
      SpringAiChatProperties properties,
      TenantSecurityAdvisor tenantSecurityAdvisor,
      PromptVersionAdvisor promptVersionAdvisor) {
    return new SpringAiChatProviderRegistry(
        chatClients,
        properties,
        List.of(tenantSecurityAdvisor, promptVersionAdvisor),
        NoopAiTraceRecorder.INSTANCE);
  }

  SpringAiChatProviderRegistry chatProviderRegistry(
      Map<String, ChatClient> chatClients,
      SpringAiChatProperties properties,
      TenantSecurityAdvisor tenantSecurityAdvisor,
      PromptVersionAdvisor promptVersionAdvisor,
      AiTraceRecorder traceRecorder) {
    return new SpringAiChatProviderRegistry(
        chatClients,
        properties,
        List.of(tenantSecurityAdvisor, promptVersionAdvisor),
        traceRecorder);
  }

  SpringAiChatProviderRegistry chatProviderRegistry(
      Map<String, ChatClient> chatClients,
      SpringAiChatProperties properties,
      TenantSecurityAdvisor tenantSecurityAdvisor,
      PromptVersionAdvisor promptVersionAdvisor,
      AiTraceRecorder traceRecorder,
      ToolCallbackProvider toolCallbackProvider) {
    return new SpringAiChatProviderRegistry(
        chatClients,
        properties,
        List.of(tenantSecurityAdvisor, promptVersionAdvisor),
        traceRecorder,
        toolCallbackProvider);
  }

  SpringAiChatProviderRegistry chatProviderRegistry(
      Map<String, ChatClient> chatClients,
      SpringAiChatProperties properties,
      List<? extends Advisor> advisors,
      AiTraceRecorder traceRecorder) {
    return new SpringAiChatProviderRegistry(chatClients, properties, advisors, traceRecorder);
  }

  SpringAiChatProviderRegistry chatProviderRegistry(
      Map<String, ChatClient> chatClients,
      SpringAiChatProperties properties,
      List<? extends Advisor> advisors,
      AiTraceRecorder traceRecorder,
      ToolCallbackProvider toolCallbackProvider) {
    return new SpringAiChatProviderRegistry(
        chatClients, properties, advisors, traceRecorder, toolCallbackProvider);
  }

  @Bean(name = "aiChatCompletion")
  @ConditionalOnMissingBean(name = "aiChatCompletion")
  ChatCompletionPort chatCompletionPort(
      SpringAiChatProviderRegistry registry,
      Optional<ModelExecutionScheduler> scheduler,
      AiExecutorProperties executionProperties) {
    return scheduler
        .map(admission -> chatCompletionPort(registry, admission, executionProperties))
        .orElseGet(() -> new ChatModelSelector(registry.providers()));
  }

  ChatCompletionPort chatCompletionPort(
      SpringAiChatProviderRegistry registry,
      ModelExecutionScheduler scheduler,
      AiExecutorProperties executionProperties) {
    return new ChatModelSelector(
        registry.providers(), scheduler, executionProperties.modelAdmissionTimeout());
  }

  ChatCompletionPort chatCompletionPort(SpringAiChatProviderRegistry registry) {
    return new ChatModelSelector(registry.providers());
  }
}
