package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.PromptVersionAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.TenantSecurityAdvisor;
import com.emme.assistant.ai.application.provider.ChatModelSelector;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientBuilderConfigurer;
import org.springframework.ai.tool.ToolCallbackProvider;

class SpringAiChatConfigurationTest {

  @Test
  void createsNamedClientsThroughTheSpringAiBuilderConfigurer() {
    SpringAiChatConfiguration configuration = new SpringAiChatConfiguration();
    ChatModel model = mock(ChatModel.class);
    ChatClientBuilderConfigurer configurer = mock(ChatClientBuilderConfigurer.class);
    ChatClient.Builder configuredBuilder = mock(ChatClient.Builder.class);
    ChatClient expectedClient = mock(ChatClient.class);
    when(configurer.configure(any(ChatClient.Builder.class))).thenReturn(configuredBuilder);
    when(configuredBuilder.build()).thenReturn(expectedClient);

    ChatClient client = configuration.ollamaChatClient(model, mock(), configurer);

    assertThat(client).isSameAs(expectedClient);
    verify(configurer).configure(any(ChatClient.Builder.class));
    verify(configuredBuilder).build();
  }

  @Test
  void treatsMissingProviderCredentialsAsUnavailableSoTheSelectorCanFallback() {
    ChatClient missingCredentialClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    ChatClient fallbackClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(missingCredentialClient.prompt().system(anyString()).user("hello").call())
        .thenThrow(new IllegalArgumentException("API key must not be blank"));
    when(fallbackClient.prompt().system(anyString()).user("hello").call().content())
        .thenReturn("hola");
    SpringAiChatProperties properties =
        new SpringAiChatProperties(
            true,
            List.of(
                new SpringAiChatProperties.Provider("missingCredential", "groq", "groq-v1"),
                new SpringAiChatProperties.Provider("fallback", "ollama", "ollama-v1")));
    SpringAiChatProviderRegistry registry =
        new SpringAiChatProviderRegistry(
            Map.of("missingCredential", missingCredentialClient, "fallback", fallbackClient),
            properties);

    AiExecutionContext context = context();
    assertThat(
            AiExecutionContextScope.call(
                context,
                () ->
                    new ChatModelSelector(registry.providers())
                        .complete(request(context, List.of("groq", "ollama")))))
        .isEqualTo(new ChatResponse("hola", "ollama", "ollama-v1", 0, 0));
  }

  @Test
  void preservesInvalidSchemaFailuresInsteadOfTreatingThemAsProviderOutages() {
    ChatClient schemaFailureClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    ChatClient fallbackClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    IllegalArgumentException schemaFailure =
        new IllegalArgumentException("invalid response schema");
    when(schemaFailureClient.prompt().system(anyString()).user("hello").call())
        .thenThrow(schemaFailure);
    SpringAiChatProperties properties =
        new SpringAiChatProperties(
            true,
            List.of(
                new SpringAiChatProperties.Provider("schemaFailure", "groq", "groq-v1"),
                new SpringAiChatProperties.Provider("fallback", "ollama", "ollama-v1")));
    SpringAiChatProviderRegistry registry =
        new SpringAiChatProviderRegistry(
            Map.of("schemaFailure", schemaFailureClient, "fallback", fallbackClient), properties);

    AiExecutionContext context = context();
    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context,
                    () ->
                        new ChatModelSelector(registry.providers())
                            .complete(request(context, List.of("groq", "ollama")))))
        .isSameAs(schemaFailure);
  }

  @Test
  void buildsAnOrderedModelSelectorFromNamedChatClients() {
    SpringAiChatProperties properties =
        new SpringAiChatProperties(
            true,
            List.of(
                new SpringAiChatProperties.Provider("localChatClient", "local", "ollama-v1"),
                new SpringAiChatProperties.Provider("cloudChatClient", "cloud", "cloud-v1")));
    SpringAiChatProviderRegistry registry =
        new SpringAiChatProviderRegistry(
            Map.of(
                "localChatClient", mock(ChatClient.class),
                "cloudChatClient", mock(ChatClient.class)),
            properties,
            List.of(new TenantSecurityAdvisor(), new PromptVersionAdvisor("chat-v1")),
            mock(AiTraceRecorder.class));

    AiChatCompletion port =
        new SpringAiChatConfiguration()
            .chatCompletionPort(registry, Optional.empty(), new AiExecutorProperties(2, 1, 1));

    assertThat(port).isInstanceOf(ChatModelSelector.class);
  }

  @Test
  void invokesConfiguredChatClientsInOrderWhenFallingBack() {
    ChatClient local = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    ChatClient cloud = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(local.prompt().system(anyString()).user("hello").call().content())
        .thenThrow(new RuntimeException("local unavailable"));
    when(cloud.prompt().system(anyString()).user("hello").call().content()).thenReturn("hola");
    SpringAiChatProperties properties =
        new SpringAiChatProperties(
            true,
            List.of(
                new SpringAiChatProperties.Provider("localChatClient", "local", "local-v1"),
                new SpringAiChatProperties.Provider("cloudChatClient", "cloud", "cloud-v1")));
    SpringAiChatConfiguration configuration = new SpringAiChatConfiguration();
    SpringAiChatProviderRegistry registry =
        new SpringAiChatProviderRegistry(
            Map.of("localChatClient", local, "cloudChatClient", cloud), properties);

    AiExecutionContext context = context();
    assertThat(
            AiExecutionContextScope.call(
                context,
                () ->
                    configuration
                        .chatCompletionPort(
                            registry, Optional.empty(), new AiExecutorProperties(2, 1, 1))
                        .complete(request(context, List.of("local", "cloud")))))
        .isEqualTo(new ChatResponse("hola", "cloud", "cloud-v1", 0, 0));
    var invocationOrder = inOrder(local, cloud);
    invocationOrder.verify(local).prompt();
    invocationOrder.verify(cloud).prompt();
  }

  @Test
  void reportsTheConfiguredProviderIdentityForAChatCompletion() {
    ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(client
            .prompt()
            .system(org.mockito.ArgumentMatchers.anyString())
            .user("hello")
            .call()
            .content())
        .thenReturn("answer");
    SpringAiChatProperties properties =
        new SpringAiChatProperties(
            true,
            List.of(
                new SpringAiChatProperties.Provider(
                    "localChatClient", "local-ollama", "ollama-chat")));
    SpringAiChatProviderRegistry registry =
        new SpringAiChatProviderRegistry(Map.of("localChatClient", client), properties);

    AiExecutionContext context = context();
    var result =
        AiExecutionContextScope.call(
            context,
            () ->
                new ChatModelSelector(registry.providers())
                    .complete(request(context, List.of("local-ollama"))));

    assertThat(result.provider()).isEqualTo("local-ollama");
    assertThat(result.modelVersion()).isEqualTo("ollama-chat");
  }

  @Test
  void rejectsAConfiguredClientThatIsMissing() {
    SpringAiChatProperties properties =
        new SpringAiChatProperties(
            true, List.of(new SpringAiChatProperties.Provider("missing", "local", "ollama-v1")));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new SpringAiChatProviderRegistry(
                    Map.of(),
                    properties,
                    List.of(new TenantSecurityAdvisor(), new PromptVersionAdvisor("chat-v1"))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No Spring AI chat client bean configured for provider 'local'");
  }

  @Test
  void wiresTheExistingModelSchedulerIntoTheModelSelector() {
    SpringAiChatConfiguration configuration = new SpringAiChatConfiguration();
    SpringAiChatProperties properties =
        new SpringAiChatProperties(
            true,
            List.of(new SpringAiChatProperties.Provider("localChatClient", "local", "ollama-v1")));
    SpringAiChatProviderRegistry registry =
        new SpringAiChatProviderRegistry(
            Map.of("localChatClient", mock(ChatClient.class)),
            properties,
            List.of(new TenantSecurityAdvisor(), new PromptVersionAdvisor("chat-v1")));

    assertThat(
            configuration.chatCompletionPort(
                registry,
                Optional.of(mock(ModelExecutionScheduler.class)),
                new AiExecutorProperties(2, 1, 1)))
        .isInstanceOf(ChatModelSelector.class);
  }

  @Test
  void passesTheBackendApprovedToolProviderToNamedChatClients() {
    SpringAiChatProperties properties =
        new SpringAiChatProperties(
            true,
            List.of(new SpringAiChatProperties.Provider("localChatClient", "local", "ollama-v1")));
    ToolCallbackProvider toolProvider = org.mockito.Mockito.mock(ToolCallbackProvider.class);

    SpringAiChatProviderRegistry registry =
        new SpringAiChatProviderRegistry(
            Map.of("localChatClient", mock(ChatClient.class)),
            properties,
            List.of(new TenantSecurityAdvisor(), new PromptVersionAdvisor("chat-v1")),
            mock(AiTraceRecorder.class),
            toolProvider);

    assertThat(registry.providers()).hasSize(1);
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), Set.of("ROLE_CLIENT"), id, id, "trace-1", "idem-1");
  }

  private static AiChatCompletion.Request request(
      AiExecutionContext context, List<String> providers) {
    return new AiChatCompletion.Request(
        "", "hello", context, new AiChatCompletion.ProviderPolicy(providers, true));
  }
}
