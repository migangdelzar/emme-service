package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatServiceProviderFallbackTest {

  @Test
  void usesTheSpringAiChatChainBeforeTheLegacyProvider() {
    AiModelProvider legacy = mock(AiModelProvider.class);
    ChatCompletionPort chain = mock(ChatCompletionPort.class);
    when(chain.complete("", "hello")).thenReturn("Spring AI response");
    ChatService service = new ChatService(legacy, Optional.empty(), Optional.of(chain));

    assertThat(AiExecutionContextScope.call(context(), () -> service.chat("", "hello")))
        .isEqualTo("Spring AI response");
  }

  @Test
  void usesTheLegacyProviderWhenTheSpringAiChainIsUnavailable() {
    AiModelProvider legacy = mock(AiModelProvider.class);
    ChatCompletionPort chain = mock(ChatCompletionPort.class);
    when(chain.complete("", "hello"))
        .thenThrow(new ChatProviderUnavailableException("providers unavailable"));
    when(legacy.chat("", "hello")).thenReturn("legacy response");
    ChatService service = new ChatService(legacy, Optional.empty(), Optional.of(chain));

    assertThat(AiExecutionContextScope.call(context(), () -> service.chat("", "hello")))
        .isEqualTo("legacy response");
    verify(legacy).chat("", "hello");
  }

  @Test
  void preservesUnavailableSemanticsWhenTheLegacyProviderAlsoHasARuntimeOutage() {
    AiModelProvider legacy = mock(AiModelProvider.class);
    ChatCompletionPort chain = mock(ChatCompletionPort.class);
    when(legacy.name()).thenReturn("groq");
    when(chain.complete("", "hello"))
        .thenThrow(new ChatProviderUnavailableException("providers unavailable"));
    RuntimeException outage = new IllegalStateException("connection refused");
    when(legacy.chat("", "hello")).thenThrow(outage);
    ChatService service = new ChatService(legacy, Optional.empty(), Optional.of(chain));

    assertThatThrownBy(
            () -> AiExecutionContextScope.call(context(), () -> service.chat("", "hello")))
        .isInstanceOf(ChatProviderUnavailableException.class)
        .hasMessage("Chat provider '" + legacy.name() + "' is unavailable")
        .hasCause(outage);
  }

  @Test
  void doesNotConvertLegacyInvalidInputOrSchemaFailuresToUnavailable() {
    AiModelProvider legacy = mock(AiModelProvider.class);
    ChatCompletionPort chain = mock(ChatCompletionPort.class);
    when(chain.complete("", "hello"))
        .thenThrow(new ChatProviderUnavailableException("providers unavailable"));
    IllegalArgumentException schemaFailure =
        new IllegalArgumentException("invalid response schema");
    when(legacy.chat("", "hello")).thenThrow(schemaFailure);
    ChatService service = new ChatService(legacy, Optional.empty(), Optional.of(chain));

    assertThatThrownBy(
            () -> AiExecutionContextScope.call(context(), () -> service.chat("", "hello")))
        .isSameAs(schemaFailure);
  }

  @Test
  void treatsMissingLegacyCredentialsAsUnavailable() {
    AiModelProvider legacy = mock(AiModelProvider.class);
    ChatCompletionPort chain = mock(ChatCompletionPort.class);
    when(legacy.name()).thenReturn("groq");
    when(chain.complete("", "hello"))
        .thenThrow(new ChatProviderUnavailableException("providers unavailable"));
    IllegalArgumentException missingCredentials =
        new IllegalArgumentException("API key must not be blank");
    when(legacy.chat("", "hello")).thenThrow(missingCredentials);
    ChatService service = new ChatService(legacy, Optional.empty(), Optional.of(chain));

    assertThatThrownBy(
            () -> AiExecutionContextScope.call(context(), () -> service.chat("", "hello")))
        .isInstanceOf(ChatProviderUnavailableException.class)
        .hasMessage("Chat provider 'groq' is unavailable")
        .hasCause(missingCredentials);
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("ROLE_client"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-chat-fallback",
        "idem-chat-fallback");
  }
}
