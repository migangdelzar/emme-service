package com.emme.ai.platform.adapter.out.capability;

import com.emme.ai.contracts.image.CaptionImageUseCase;
import com.emme.ai.contracts.model.AiModelProvider;
import org.springframework.stereotype.Service;

/** Adapts the provider-neutral image capability to the configured model provider. */
@Service
public class AiCaptionImageAdapter implements CaptionImageUseCase {

  private final AiModelProvider provider;

  public AiCaptionImageAdapter(AiModelProvider provider) {
    this.provider = provider;
  }

  @Override
  public String caption(String imageBase64) {
    return provider.caption(imageBase64);
  }
}
