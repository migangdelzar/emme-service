package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.api.usecase.CaptionImageUseCase;
import com.emme.assistant.ai.application.port.out.ModelProvider;
import org.springframework.stereotype.Service;

/** Executes image captioning through the configured model-provider port. */
@Service
public class CaptionImageService implements CaptionImageUseCase {

  private final ModelProvider provider;

  public CaptionImageService(ModelProvider provider) {
    this.provider = provider;
  }

  @Override
  public String caption(String imageBase64) {
    return provider.caption(imageBase64);
  }
}
