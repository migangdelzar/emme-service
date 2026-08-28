package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.ModelProvider;
import com.emme.assistant.ai.application.port.out.SemanticResponseCache;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChatServiceTest {

  @Test
  void returnsAHighConfidenceCacheHitWithoutCallingTheModel() {
    ModelProvider model = mock(ModelProvider.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    when(cache.lookup("", "What are your hours?")).thenReturn(Optional.of("Open today."));
    ChatService service = new ChatService(model, Optional.of(cache));

    assertThat(service.chat("", "What are your hours?")).isEqualTo("Open today.");

    verifyNoInteractions(model);
  }

  @Test
  void storesAProviderResponseAfterASemanticCacheMiss() {
    ModelProvider model = mock(ModelProvider.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    when(cache.lookup("", "What are your hours?")).thenReturn(Optional.empty());
    when(model.chat("", "What are your hours?")).thenReturn("Open today.");
    ChatService service = new ChatService(model, Optional.of(cache));

    assertThat(service.chat("", "What are your hours?")).isEqualTo("Open today.");

    verify(cache).store("", "What are your hours?", "Open today.");
  }

  @Test
  void preservesExistingModelBehaviorWhenSemanticCachingIsDisabled() {
    ModelProvider model = mock(ModelProvider.class);
    when(model.chat("context", "hello")).thenReturn("response");
    ChatService service = new ChatService(model, Optional.empty());

    assertThat(service.chat("context", "hello")).isEqualTo("response");
    verify(model).chat("context", "hello");
  }
}
