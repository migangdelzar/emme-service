package com.emme.assistant.ai.api.usecase;

import java.util.UUID;

public interface RagQueryUseCase {
  String query(UUID tenantId, String question);
}
