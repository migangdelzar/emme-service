package com.emme.assistant.ai.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.adapter.out.provider.springai.advisor.PromptVersionAdvisor;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;

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

  @Test
  void suppliesTheBackendApprovedToolCatalogToSpringAi() {
    ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
    ToolCallbackProvider toolProvider = mock(ToolCallbackProvider.class);
    ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
    when(client.prompt()).thenReturn(request);
    when(request.tools(toolProvider)).thenReturn(request);
    when(request.system(org.mockito.ArgumentMatchers.anyString())).thenReturn(request);
    when(request.user("hello")).thenReturn(request);
    when(request.call()).thenReturn(response);
    when(response.content()).thenReturn("Hola");
    SpringAiChatClientAdapter adapter =
        new SpringAiChatClientAdapter(client, "cloud", List.of(), toolProvider);

    assertThat(adapter.complete("", "hello")).isEqualTo("Hola");
    verify(request).tools(toolProvider);
  }

  @Test
  void scopesToolSearchIndexSessionsToTheBackendIdentityAndRoleSet() {
    ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
    ChatClient.AdvisorSpec advisorSpec = mock(ChatClient.AdvisorSpec.class);
    ToolSearchToolCallingAdvisor toolSearchAdvisor = mock(ToolSearchToolCallingAdvisor.class);
    ToolCallbackProvider toolProvider = mock(ToolCallbackProvider.class);
    ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
    AiExecutionContext context =
        new AiExecutionContext(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Set.of("ROLE_CLIENT", "ROLE_TENANT_STAFF"),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "trace-tool-search",
            "idempotency-tool-search");
    when(client.prompt()).thenReturn(request);
    when(request.advisors(org.mockito.ArgumentMatchers.<Consumer<ChatClient.AdvisorSpec>>any()))
        .thenAnswer(
            invocation -> {
              invocation.<Consumer<ChatClient.AdvisorSpec>>getArgument(0).accept(advisorSpec);
              return request;
            });
    when(advisorSpec.advisors(org.mockito.ArgumentMatchers.anyList())).thenReturn(advisorSpec);
    when(advisorSpec.param(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(advisorSpec);
    when(request.tools(toolProvider)).thenReturn(request);
    when(request.system(org.mockito.ArgumentMatchers.anyString())).thenReturn(request);
    when(request.user("hello")).thenReturn(request);
    when(request.call()).thenReturn(response);
    when(response.content()).thenReturn("Hola");
    SpringAiChatClientAdapter adapter =
        new SpringAiChatClientAdapter(client, "local", List.of(toolSearchAdvisor), toolProvider);

    assertThat(AiExecutionContextScope.call(context, () -> adapter.complete("", "hello")))
        .isEqualTo("Hola");

    org.mockito.ArgumentCaptor<Object> sessionId =
        org.mockito.ArgumentCaptor.forClass(Object.class);
    verify(advisorSpec)
        .param(org.mockito.ArgumentMatchers.eq(ChatMemory.CONVERSATION_ID), sessionId.capture());
    assertThat(sessionId.getValue().toString())
        .startsWith(
            context.tenantId()
                + ":"
                + context.principalId()
                + ":"
                + context.conversationId()
                + ":");
  }
}
