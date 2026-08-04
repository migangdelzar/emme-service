package com.emme.assistant.ai.api.usecase;

import com.emme.assistant.ai.api.result.IntentResult;

public interface DetectIntentUseCase {
  IntentResult detect(String message);
}
