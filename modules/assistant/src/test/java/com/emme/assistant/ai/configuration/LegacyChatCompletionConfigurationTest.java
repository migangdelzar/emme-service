package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.ai.contracts.model.ModelCapability;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.PromptVersionAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.TenantSecurityAdvisor;
import com.emme.assistant.ai.application.port.out.IdentifiedChatCompletionPort;
import com.emme.assistant.ai.application.provider.ChatModelSelector;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class LegacyChatCompletionConfigurationTest {

  @Test
  void adaptsTheLegacyProviderThroughAdmissionAndDurableTracing() {
    AiChatCompletion provider = mock(AiChatCompletion.class);
    ModelExecutionScheduler scheduler = new InlineScheduler();
    AiTraceRecorder recorder = mock(AiTraceRecorder.class);
    when(provider.complete(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new ChatResponse("response", "mock", "mock-v1", 0, 0));
    LegacyChatCompletionConfiguration configuration = new LegacyChatCompletionConfiguration();

    IdentifiedChatCompletionPort port =
        configuration.legacyChatCompletion(
            provider, properties(), scheduler, new AiExecutorProperties(2, 1, 1), recorder);

    var result =
        AiExecutionContextScope.call(context(), () -> port.completeWithIdentity("", "hello"));

    assertThat(result)
        .isEqualTo(
            new IdentifiedChatCompletionPort.ChatCompletionResult(
                "response", "mock", "legacy-model"));
    verify(provider).complete(org.mockito.ArgumentMatchers.any());
    verify(recorder).recordModelExecution(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void createsTheCompatibilityPortWhenSpringChatIsNotSelected() {
    new ApplicationContextRunner()
        .withUserConfiguration(LegacyChatCompletionConfiguration.class)
        .withBean(
            "legacyAiChatCompletion",
            AiChatCompletion.class,
            LegacyChatCompletionConfigurationTest::mockProvider)
        .withBean(AiProviderProperties.class, LegacyChatCompletionConfigurationTest::properties)
        .withBean(ModelExecutionScheduler.class, () -> mock(ModelExecutionScheduler.class))
        .withBean(AiExecutorProperties.class, () -> new AiExecutorProperties(2, 1, 1))
        .withBean(AiTraceRecorder.class, () -> mock(AiTraceRecorder.class))
        .run(context -> assertThat(context).hasSingleBean(IdentifiedChatCompletionPort.class));
  }

  @Test
  void exposesTheConfiguredSelectorAsThePrimaryCanonicalChatCapability() {
    new ApplicationContextRunner()
        .withUserConfiguration(LegacyChatCompletionConfiguration.class)
        .withBean(
            "legacyAiChatCompletion",
            AiChatCompletion.class,
            LegacyChatCompletionConfigurationTest::mockProvider)
        .withBean(AiProviderProperties.class, LegacyChatCompletionConfigurationTest::properties)
        .withBean(ModelExecutionScheduler.class, () -> mock(ModelExecutionScheduler.class))
        .withBean(AiExecutorProperties.class, () -> new AiExecutorProperties(2, 1, 1))
        .withBean(AiTraceRecorder.class, () -> mock(AiTraceRecorder.class))
        .run(
            context ->
                assertThat(context.getBean(AiChatCompletion.class))
                    .isInstanceOf(ChatModelSelector.class));
  }

  @Test
  void doesNotCreateTheCompatibilityPortWhenSpringChatRootIsPresent() {
    new ApplicationContextRunner()
        .withUserConfiguration(LegacyChatCompletionConfiguration.class, SpringChatRoot.class)
        .withPropertyValues("app.ai.spring-chat.enabled=true")
        .withBean(
            "legacyAiChatCompletion",
            AiChatCompletion.class,
            LegacyChatCompletionConfigurationTest::mockProvider)
        .withBean(AiProviderProperties.class, LegacyChatCompletionConfigurationTest::properties)
        .withBean(ModelExecutionScheduler.class, () -> mock(ModelExecutionScheduler.class))
        .withBean(AiExecutorProperties.class, () -> new AiExecutorProperties(2, 1, 1))
        .withBean(AiTraceRecorder.class, () -> mock(AiTraceRecorder.class))
        .run(
            context ->
                assertThat(context)
                    .hasSingleBean(IdentifiedChatCompletionPort.class)
                    .hasBean("aiChatCompletion")
                    .doesNotHaveBean("aiLegacyChatCompletion"));
  }

  @Test
  void usesTheRealSpringChatRootWhenSpringChatIsEnabled() {
    new ApplicationContextRunner()
        .withUserConfiguration(
            SpringAiChatConfiguration.class, LegacyChatCompletionConfiguration.class)
        .withPropertyValues(
            "app.ai.spring-chat.enabled=true",
            "app.ai.spring-chat.providers[0].bean-name=ollamaChatClient",
            "app.ai.spring-chat.providers[0].key=local",
            "app.ai.spring-chat.providers[0].model-version=ollama-v1")
        .withBean("ollamaChatClient", ChatClient.class, () -> mock(ChatClient.class))
        .withBean(ObservationRegistry.class, () -> ObservationRegistry.NOOP)
        .withBean(AiExecutorProperties.class, () -> new AiExecutorProperties(2, 1, 1))
        .withBean(
            AiProviderProperties.class,
            () ->
                new AiProviderProperties(
                    "mock",
                    new AiProviderProperties.ProviderConfig("model", "http://localhost", null),
                    null,
                    true))
        .withBean(TenantSecurityAdvisor.class, TenantSecurityAdvisor::new)
        .withBean(PromptVersionAdvisor.class, () -> new PromptVersionAdvisor("chat-v1"))
        .withBean(AiTraceRecorder.class, () -> mock(AiTraceRecorder.class))
        .run(
            context ->
                assertThat(context)
                    .hasSingleBean(IdentifiedChatCompletionPort.class)
                    .hasBean("aiChatCompletion")
                    .doesNotHaveBean("aiLegacyChatCompletion"));
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("ROLE_CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-legacy-chat",
        "idempotency-legacy-chat");
  }

  private static AiChatCompletion mockProvider() {
    AiChatCompletion provider = mock(AiChatCompletion.class);
    return provider;
  }

  private static AiProviderProperties properties() {
    return new AiProviderProperties("mock", null, null, true);
  }

  private static final class InlineScheduler implements ModelExecutionScheduler {
    @Override
    public <T> T execute(
        ModelCapability capability,
        AiExecutionContext context,
        Duration timeout,
        Callable<T> operation) {
      assertThat(capability).isEqualTo(ModelCapability.GENERATION);
      return call(operation);
    }

    private static <T> T call(Callable<T> operation) {
      try {
        return operation.call();
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }

  @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
  static class SpringChatRoot {
    @org.springframework.context.annotation.Bean(name = "aiChatCompletion")
    IdentifiedChatCompletionPort aiChatCompletion() {
      return mock(IdentifiedChatCompletionPort.class);
    }
  }
}
