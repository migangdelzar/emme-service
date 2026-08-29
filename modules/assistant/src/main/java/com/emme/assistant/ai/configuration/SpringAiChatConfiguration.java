package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.adapter.out.provider.springai.SpringAiToolCallbackProvider;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.PromptVersionAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.TenantSecurityAdvisor;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.provider.ChatProviderChain;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;
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

  @Bean(name = "ollamaChatModel")
  @ConditionalOnMissingBean(name = "ollamaChatModel")
  ChatModel ollamaChatModel(AiProperties aiProperties) {
    return OllamaChatModel.builder()
        .ollamaApi(OllamaApi.builder().baseUrl(aiProperties.chat().baseUrl()).build())
        .options(OllamaChatOptions.builder().model(aiProperties.chat().model()).build())
        .build();
  }

  @Bean(name = "ollamaChatClient")
  @ConditionalOnMissingBean(name = "ollamaChatClient")
  ChatClient ollamaChatClient(ChatModel ollamaChatModel) {
    return ChatClient.builder(ollamaChatModel).build();
  }

  @Bean
  SpringAiChatProviderRegistry chatProviderRegistry(
      Map<String, ChatClient> chatClients,
      SpringAiChatProperties properties,
      TenantSecurityAdvisor tenantSecurityAdvisor,
      PromptVersionAdvisor promptVersionAdvisor,
      AiTraceRecorder traceRecorder,
      Optional<SpringAiToolCallbackProvider> toolCallbackProvider) {
    return toolCallbackProvider
        .<SpringAiChatProviderRegistry>map(
            provider ->
                chatProviderRegistry(
                    chatClients,
                    properties,
                    tenantSecurityAdvisor,
                    promptVersionAdvisor,
                    traceRecorder,
                    provider))
        .orElseGet(
            () ->
                chatProviderRegistry(
                    chatClients,
                    properties,
                    tenantSecurityAdvisor,
                    promptVersionAdvisor,
                    traceRecorder));
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

  @Bean(name = "aiChatCompletion")
  @ConditionalOnMissingBean(name = "aiChatCompletion")
  ChatCompletionPort chatCompletionPort(
      SpringAiChatProviderRegistry registry,
      Optional<ModelExecutionScheduler> scheduler,
      AiExecutorProperties executionProperties) {
    return scheduler
        .map(admission -> chatCompletionPort(registry, admission, executionProperties))
        .orElseGet(() -> new ChatProviderChain(registry.providers()));
  }

  ChatCompletionPort chatCompletionPort(
      SpringAiChatProviderRegistry registry,
      ModelExecutionScheduler scheduler,
      AiExecutorProperties executionProperties) {
    return new ChatProviderChain(
        registry.providers(), scheduler, executionProperties.modelAdmissionTimeout());
  }

  ChatCompletionPort chatCompletionPort(SpringAiChatProviderRegistry registry) {
    return new ChatProviderChain(registry.providers());
  }
}
