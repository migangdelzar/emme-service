package com.emme.assistant.ai.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.adapter.out.provider.springai.advisor.PromptVersionAdvisor;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;

class SpringAiChatClientAdapterTest {

  @Test
  void mapsAChatClientResponseToTheProviderNeutralPort() {
    ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(client
            .prompt()
            .system(org.mockito.ArgumentMatchers.anyString())
            .user("hello")
            .call()
            .content())
        .thenReturn("Hola");
    SpringAiChatClientAdapter adapter = new SpringAiChatClientAdapter(client, "ollama");

    assertThat(adapter.complete("", "hello")).isEqualTo("Hola");
  }

  @Test
  void mapsProviderRuntimeFailuresToAnExplicitUnavailableFailure() {
    ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    when(client.prompt().system(org.mockito.ArgumentMatchers.anyString()).user("hello").call())
        .thenThrow(new RuntimeException("connection refused"));
    SpringAiChatClientAdapter adapter = new SpringAiChatClientAdapter(client, "ollama");

    assertThatThrownBy(() -> adapter.complete("", "hello"))
        .isInstanceOf(ChatProviderUnavailableException.class)
        .hasMessage("Chat provider 'ollama' is unavailable");
  }

  @Test
  void appliesMandatoryAdvisorsToEveryNamedProviderRequest() {
    ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
    Advisor advisor = new PromptVersionAdvisor("chat-v1");
    ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
    when(client.prompt()).thenReturn(request);
    when(request.advisors(List.of(advisor))).thenReturn(request);
    when(request.system(org.mockito.ArgumentMatchers.anyString())).thenReturn(request);
    when(request.user("hello")).thenReturn(request);
    when(request.call()).thenReturn(response);
    when(response.content()).thenReturn("Hola");
    SpringAiChatClientAdapter adapter =
        new SpringAiChatClientAdapter(client, "cloud", List.of(advisor));

    assertThat(adapter.complete("", "hello")).isEqualTo("Hola");
    verify(request).advisors(List.of(advisor));
  }
}
