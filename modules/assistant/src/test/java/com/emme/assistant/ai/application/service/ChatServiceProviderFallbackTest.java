package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
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
