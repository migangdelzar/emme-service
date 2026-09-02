package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.PromptVersionAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.TenantSecurityAdvisor;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.provider.ChatModelSelector;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;

class SpringAiChatConfigurationTest {

  @Test
  void buildsAnOrderedModelSelectorFromNamedChatClients() {
    SpringAiChatConfiguration configuration = new SpringAiChatConfiguration();
    SpringAiChatProperties properties =
        new SpringAiChatProperties(
            true,
            List.of(
                new SpringAiChatProperties.Provider("localChatClient", "local", "ollama-v1"),
                new SpringAiChatProperties.Provider("cloudChatClient", "cloud", "cloud-v1")));

    ChatCompletionPort port =
        configuration.chatCompletionPort(
            configuration.chatProviderRegistry(
                Map.of(
                    "localChatClient",
                    mock(ChatClient.class),
                    "cloudChatClient",
                    mock(ChatClient.class)),
                properties,
                new TenantSecurityAdvisor(),
                new PromptVersionAdvisor("chat-v1"),
                mock(AiTraceRecorder.class)));

    assertThat(port).isInstanceOf(ChatModelSelector.class);
  }

  @Test
  void reportsTheConfiguredProviderIdentityForAChatCompletion() {
    ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(
            client
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

    var result = new ChatModelSelector(registry.providers()).completeWithIdentity("", "hello");

    assertThat(result.provider()).isEqualTo("local-ollama");
    assertThat(result.model()).isEqualTo("ollama-chat");
  }

  @Test
  void rejectsAConfiguredClientThatIsMissing() {
    SpringAiChatConfiguration configuration = new SpringAiChatConfiguration();
    SpringAiChatProperties properties =
        new SpringAiChatProperties(
            true, List.of(new SpringAiChatProperties.Provider("missing", "local", "ollama-v1")));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                configuration.chatProviderRegistry(
                    Map.of(),
                    properties,
                    new TenantSecurityAdvisor(),
                    new PromptVersionAdvisor("chat-v1")))
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
        configuration.chatProviderRegistry(
            Map.of("localChatClient", mock(ChatClient.class)),
            properties,
            new TenantSecurityAdvisor(),
            new PromptVersionAdvisor("chat-v1"));

    assertThat(
            configuration.chatCompletionPort(
                registry, mock(ModelExecutionScheduler.class), new AiExecutorProperties(2, 1, 1)))
        .isInstanceOf(ChatModelSelector.class);
  }

  @Test
  void passesTheBackendApprovedToolProviderToNamedChatClients() {
    SpringAiChatConfiguration configuration = new SpringAiChatConfiguration();
    SpringAiChatProperties properties =
        new SpringAiChatProperties(
            true,
            List.of(new SpringAiChatProperties.Provider("localChatClient", "local", "ollama-v1")));
    ToolCallbackProvider toolProvider = org.mockito.Mockito.mock(ToolCallbackProvider.class);

    SpringAiChatProviderRegistry registry =
        configuration.chatProviderRegistry(
            Map.of("localChatClient", mock(ChatClient.class)),
            properties,
            new TenantSecurityAdvisor(),
            new PromptVersionAdvisor("chat-v1"),
            mock(AiTraceRecorder.class),
            toolProvider);

    assertThat(registry.providers()).hasSize(1);
  }
}
