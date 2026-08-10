package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.ModelProvider;
import org.junit.jupiter.api.Test;

class CaptionImageServiceTest {

  private final ModelProvider provider = mock(ModelProvider.class);

  @Test
  void delegatesCaptioningToTheConfiguredModelProvider() {
    when(provider.caption("base64-image")).thenReturn("a red chair");

    String caption = new CaptionImageService(provider).caption("base64-image");

    assertThat(caption).isEqualTo("a red chair");
  }
}
