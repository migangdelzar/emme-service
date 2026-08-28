package com.emme.ai.platform.adapter.out.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.AiModelProvider;
import org.junit.jupiter.api.Test;

class AiCaptionImageAdapterTest {

  private final AiModelProvider provider = mock(AiModelProvider.class);

  @Test
  void delegatesCaptioningToTheConfiguredModelProvider() {
    when(provider.caption("base64-image")).thenReturn("a red chair");

    String caption = new AiCaptionImageAdapter(provider).caption("base64-image");

    assertThat(caption).isEqualTo("a red chair");
  }
}
