package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatServiceProviderFallbackTest {

  @Test
  void usesTheCanonicalChatPortForTheConfiguredResponse() {
    AiChatCompletion canonical = mock(AiChatCompletion.class);
    when(canonical.complete(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new ChatResponse("canonical response", "test", "test-v1", 0, 0));
    ChatService service = new ChatService(canonical, Optional.empty());

    assertThat(AiExecutionContextScope.call(context(), () -> service.chat("", "hello")))
        .isEqualTo("canonical response");

    verify(canonical).complete(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void doesNotBypassTheCanonicalPortWhenItReportsProviderUnavailability() {
    AiChatCompletion canonical = mock(AiChatCompletion.class);
    ChatProviderUnavailableException unavailable =
        new ChatProviderUnavailableException("providers unavailable");
    when(canonical.complete(org.mockito.ArgumentMatchers.any())).thenThrow(unavailable);
    ChatService service = new ChatService(canonical, Optional.empty());

    assertThatThrownBy(
            () -> AiExecutionContextScope.call(context(), () -> service.chat("", "hello")))
        .isSameAs(unavailable);

    verify(canonical).complete(org.mockito.ArgumentMatchers.any());
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("ROLE_client"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-chat-canonical",
        "idem-chat-canonical");
  }
}
