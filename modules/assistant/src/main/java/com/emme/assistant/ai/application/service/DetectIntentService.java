package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.api.result.IntentInfo;
import com.emme.assistant.ai.api.usecase.DetectIntentUseCase;
import com.emme.assistant.ai.application.port.out.ModelProvider;
import org.springframework.stereotype.Service;

@Service
public class DetectIntentService implements DetectIntentUseCase {
  private final ModelProvider provider;

  public DetectIntentService(ModelProvider provider) {
    this.provider = provider;
  }

  @Override
  public IntentInfo detect(String message) {
    ModelProvider.IntentResult result = provider.routeIntent(message);
    return new IntentInfo(result.intent(), result.confidence(), result.parameters());
  }
}
