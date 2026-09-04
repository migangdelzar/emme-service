package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.IdentifiedChatCompletionPort;
import com.emme.assistant.ai.application.provider.ChatModelSelector;
import com.emme.assistant.ai.application.provider.ChatProviderFailurePolicy;
import com.emme.assistant.ai.application.provider.TracingChatCompletionPort;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Keeps the pre-Spring-chat provider available through the canonical chat boundary. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "app.ai.spring-chat",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class LegacyChatCompletionConfiguration {

  @Bean(name = "aiLegacyChatCompletion")
  @ConditionalOnMissingBean(IdentifiedChatCompletionPort.class)
  IdentifiedChatCompletionPort legacyChatCompletion(
      AiModelProvider provider,
      ModelExecutionScheduler scheduler,
      AiExecutorProperties executionProperties,
      AiTraceRecorder traceRecorder) {
    ChatCompletionPort legacyModel =
        (conversationContext, userMessage) -> {
          try {
            return provider.chat(conversationContext, userMessage);
          } catch (RuntimeException failure) {
            throw ChatProviderFailurePolicy.preserveInputOrUnavailable(provider.name(), failure);
          }
        };
    ChatCompletionPort tracedModel =
        new TracingChatCompletionPort(
            legacyModel, provider.name(), "legacy-model", "chat-v1", traceRecorder);
    return new ChatModelSelector(
        List.of(new ChatModelSelector.Provider(provider.name(), tracedModel, "legacy-model")),
        scheduler,
        executionProperties.modelAdmissionTimeout());
  }
}
