package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.api.usecase.EmbedTextUseCase;
import com.emme.assistant.ai.application.port.out.ModelProvider;
import java.util.List;
import org.springframework.stereotype.Service;

/** Executes text embedding through the configured model-provider port. */
@Service
public class EmbedTextService implements EmbedTextUseCase {

  private final ModelProvider provider;

  public EmbedTextService(ModelProvider provider) {
    this.provider = provider;
  }

  @Override
  public List<Float> embed(String text) {
    return provider.embed(text);
  }
}
