package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.assistant.ai.application.provider.ChatProviderChain;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatProviderChainTest {

  @Test
  void returnsTheFirstHealthyProviderResponse() {
    ChatCompletionPort local = mock(ChatCompletionPort.class);
    ChatCompletionPort cloud = mock(ChatCompletionPort.class);
    when(local.complete("", "hello")).thenReturn("hola");
    ChatProviderChain chain =
        new ChatProviderChain(
            List.of(
                new ChatProviderChain.Provider("local", local),
                new ChatProviderChain.Provider("cloud", cloud)));

    assertThat(chain.complete("", "hello")).isEqualTo("hola");
    verifyNoInteractions(cloud);
  }

  @Test
  void fallsBackOnlyWhenTheCurrentProviderIsUnavailable() {
    ChatCompletionPort local = mock(ChatCompletionPort.class);
    ChatCompletionPort cloud = mock(ChatCompletionPort.class);
    when(local.complete("", "hello"))
        .thenThrow(new ChatProviderUnavailableException("local unavailable"));
    when(cloud.complete("", "hello")).thenReturn("hola");
    ChatProviderChain chain =
        new ChatProviderChain(
            List.of(
                new ChatProviderChain.Provider("local", local),
                new ChatProviderChain.Provider("cloud", cloud)));

    assertThat(chain.complete("", "hello")).isEqualTo("hola");
  }

  @Test
  void reportsWhenEveryProviderIsUnavailable() {
    ChatCompletionPort local = mock(ChatCompletionPort.class);
    when(local.complete("", "hello"))
        .thenThrow(new ChatProviderUnavailableException("local unavailable"));
    ChatProviderChain chain =
        new ChatProviderChain(List.of(new ChatProviderChain.Provider("local", local)));

    assertThatThrownBy(() -> chain.complete("", "hello"))
        .isInstanceOf(ChatProviderUnavailableException.class)
        .hasMessage("All configured chat providers are unavailable: local");
  }
}
