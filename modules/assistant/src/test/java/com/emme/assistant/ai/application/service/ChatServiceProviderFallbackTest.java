package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.assistant.ai.application.port.out.ModelProvider;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChatServiceProviderFallbackTest {

  @Test
  void usesTheSpringAiChatChainBeforeTheLegacyProvider() {
    ModelProvider legacy = mock(ModelProvider.class);
    ChatCompletionPort chain = mock(ChatCompletionPort.class);
    when(chain.complete("", "hello")).thenReturn("Spring AI response");
    ChatService service = new ChatService(legacy, Optional.empty(), Optional.of(chain));

    assertThat(service.chat("", "hello")).isEqualTo("Spring AI response");
  }

  @Test
  void usesTheLegacyProviderWhenTheSpringAiChainIsUnavailable() {
    ModelProvider legacy = mock(ModelProvider.class);
    ChatCompletionPort chain = mock(ChatCompletionPort.class);
    when(chain.complete("", "hello"))
        .thenThrow(new ChatProviderUnavailableException("providers unavailable"));
    when(legacy.chat("", "hello")).thenReturn("legacy response");
    ChatService service = new ChatService(legacy, Optional.empty(), Optional.of(chain));

    assertThat(service.chat("", "hello")).isEqualTo("legacy response");
    verify(legacy).chat("", "hello");
  }
}
