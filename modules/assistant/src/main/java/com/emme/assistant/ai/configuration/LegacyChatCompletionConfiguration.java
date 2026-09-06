package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.IdentifiedChatCompletionPort;
import com.emme.assistant.ai.application.provider.ChatModelSelector;
import com.emme.assistant.ai.application.provider.ChatProviderFailurePolicy;
import com.emme.assistant.ai.application.provider.TracingChatCompletionPort;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Keeps the pre-Spring-chat provider available through the canonical chat boundary. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "app.ai.spring-chat",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class LegacyChatCompletionConfiguration {

  @Bean(name = "aiLegacyChatCompletion")
  @Primary
  @ConditionalOnMissingBean(IdentifiedChatCompletionPort.class)
  ChatModelSelector legacyChatCompletion(
      AiChatCompletion completion,
      AiProviderProperties properties,
      ModelExecutionScheduler scheduler,
      AiExecutorProperties executionProperties,
      AiTraceRecorder traceRecorder) {
    String providerKey = properties.provider();
    ChatCompletionPort legacyModel =
        (conversationContext, userMessage) -> {
          try {
            var context = AiExecutionContextScope.requireCurrent();
            ChatResponse response =
                completion.complete(
                    new AiChatCompletion.Request(
                        conversationContext,
                        userMessage,
                        context,
                        new AiChatCompletion.ProviderPolicy(List.of(providerKey), false)));
            return response.content();
          } catch (RuntimeException failure) {
            throw ChatProviderFailurePolicy.preserveInputOrUnavailable(providerKey, failure);
          }
        };
    ChatCompletionPort tracedModel =
        new TracingChatCompletionPort(
            legacyModel, providerKey, "legacy-model", "chat-v1", traceRecorder);
    return new ChatModelSelector(
        List.of(new ChatModelSelector.Provider(providerKey, tracedModel, "legacy-model")),
        scheduler,
        executionProperties.modelAdmissionTimeout());
  }
}
