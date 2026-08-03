package com.emme.assistant.ai.api.usecase;

import com.emme.assistant.ai.api.result.IntentInfo;

public interface DetectIntentUseCase {
  IntentInfo detect(String message);
}
