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

class DefaultChatCompletionConfigurationTest {

  @Test
  void adaptsTheDefaultProviderThroughAdmissionAndDurableTracing() {
    AiChatCompletion provider = mock(AiChatCompletion.class);
    ModelExecutionScheduler scheduler = new InlineScheduler();
    AiTraceRecorder recorder = mock(AiTraceRecorder.class);
    when(provider.complete(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new ChatResponse("response", "mock", "mock-v1", 0, 0));
    DefaultChatCompletionConfiguration configuration = new DefaultChatCompletionConfiguration();

    AiChatCompletion port =
        configuration.defaultChatCompletion(
            provider, properties(), scheduler, new AiExecutorProperties(2, 1, 1), recorder);

    AiExecutionContext context = context();
    var result =
        AiExecutionContextScope.call(
            context,
            () ->
                port.complete(
                    new AiChatCompletion.Request(
                        "",
                        "hello",
                        context,
                        new AiChatCompletion.ProviderPolicy(java.util.List.of("mock"), true))));

    assertThat(result).isEqualTo(new ChatResponse("response", "mock", "default-model", 0, 0));
    verify(provider).complete(org.mockito.ArgumentMatchers.any());
    verify(recorder).recordModelExecution(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void createsTheDefaultCompositionWhenSpringChatIsNotSelected() {
    new ApplicationContextRunner()
        .withUserConfiguration(DefaultChatCompletionConfiguration.class)
        .withBean(
            "providerChatCompletion",
            AiChatCompletion.class,
            DefaultChatCompletionConfigurationTest::mockProvider)
        .withBean(AiProviderProperties.class, DefaultChatCompletionConfigurationTest::properties)
        .withBean(ModelExecutionScheduler.class, () -> mock(ModelExecutionScheduler.class))
        .withBean(AiExecutorProperties.class, () -> new AiExecutorProperties(2, 1, 1))
        .withBean(AiTraceRecorder.class, () -> mock(AiTraceRecorder.class))
        .run(
            context ->
                assertThat(context.getBean(AiChatCompletion.class))
                    .isInstanceOf(ChatModelSelector.class));
  }

  @Test
  void exposesTheConfiguredSelectorAsThePrimaryCanonicalChatCapability() {
    new ApplicationContextRunner()
        .withUserConfiguration(DefaultChatCompletionConfiguration.class)
        .withBean(
            "providerChatCompletion",
            AiChatCompletion.class,
            DefaultChatCompletionConfigurationTest::mockProvider)
        .withBean(AiProviderProperties.class, DefaultChatCompletionConfigurationTest::properties)
        .withBean(ModelExecutionScheduler.class, () -> mock(ModelExecutionScheduler.class))
        .withBean(AiExecutorProperties.class, () -> new AiExecutorProperties(2, 1, 1))
        .withBean(AiTraceRecorder.class, () -> mock(AiTraceRecorder.class))
        .run(
            context ->
                assertThat(context.getBean(AiChatCompletion.class))
                    .isInstanceOf(ChatModelSelector.class));
  }

  @Test
  void doesNotCreateTheDefaultCompositionWhenSpringChatRootIsPresent() {
    new ApplicationContextRunner()
        .withUserConfiguration(DefaultChatCompletionConfiguration.class, SpringChatRoot.class)
        .withPropertyValues("app.ai.spring-chat.enabled=true")
        .withBean(
            "providerChatCompletion",
            AiChatCompletion.class,
            DefaultChatCompletionConfigurationTest::mockProvider)
        .withBean(AiProviderProperties.class, DefaultChatCompletionConfigurationTest::properties)
        .withBean(ModelExecutionScheduler.class, () -> mock(ModelExecutionScheduler.class))
        .withBean(AiExecutorProperties.class, () -> new AiExecutorProperties(2, 1, 1))
        .withBean(AiTraceRecorder.class, () -> mock(AiTraceRecorder.class))
        .run(
            context ->
                assertThat(context)
                    .hasBean("aiChatCompletion")
                    .doesNotHaveBean("defaultChatCompletion"));
  }

  @Test
  void usesTheRealSpringChatRootWhenSpringChatIsEnabled() {
    new ApplicationContextRunner()
        .withUserConfiguration(
            SpringAiChatConfiguration.class, DefaultChatCompletionConfiguration.class)
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
                    .hasBean("aiChatCompletion")
                    .doesNotHaveBean("defaultChatCompletion"));
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("ROLE_CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-default-chat",
        "idempotency-default-chat");
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
    AiChatCompletion aiChatCompletion() {
      return mock(AiChatCompletion.class);
    }
  }
}
