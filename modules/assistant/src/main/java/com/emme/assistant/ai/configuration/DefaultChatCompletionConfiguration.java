package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.assistant.ai.application.provider.ChatModelSelector;
import com.emme.assistant.ai.application.provider.ChatProviderFailurePolicy;
import com.emme.assistant.ai.application.provider.TracingAiChatCompletion;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Provides the basic provider composition when enhanced Spring AI chat is disabled. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "app.ai.spring-chat",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class DefaultChatCompletionConfiguration {

  @Bean(name = "defaultChatCompletion")
  @Primary
  @ConditionalOnMissingBean(name = "aiChatCompletion")
  ChatModelSelector defaultChatCompletion(
      AiChatCompletion completion,
      AiProviderProperties properties,
      ModelExecutionScheduler scheduler,
      AiExecutorProperties executionProperties,
      AiTraceRecorder traceRecorder) {
    String providerKey = properties.provider();
    AiChatCompletion defaultModel =
        request -> {
          try {
            ChatResponse response =
                completion.complete(
                    new AiChatCompletion.Request(
                        request.conversationContext(),
                        request.userMessage(),
                        request.executionContext(),
                        new AiChatCompletion.ProviderPolicy(List.of(providerKey), false)));
            return new ChatResponse(
                response.content(),
                providerKey,
                "default-model",
                response.inputTokens(),
                response.outputTokens());
          } catch (RuntimeException failure) {
            throw ChatProviderFailurePolicy.preserveInputOrUnavailable(providerKey, failure);
          }
        };
    AiChatCompletion tracedModel =
        new TracingAiChatCompletion(
            defaultModel, providerKey, "default-model", "chat-v1", traceRecorder);
    return new ChatModelSelector(
        List.of(new ChatModelSelector.Provider(providerKey, tracedModel, "default-model")),
        scheduler,
        executionProperties.modelAdmissionTimeout());
  }

  @Bean(name = "aiChatProviderPolicy")
  @Primary
  @ConditionalOnMissingBean(name = "aiChatProviderPolicy")
  AiChatCompletion.ProviderPolicy defaultChatProviderPolicy(AiProviderProperties properties) {
    return new AiChatCompletion.ProviderPolicy(List.of(properties.provider()), true);
  }
}
