package com.emme.assistant.ai.api.usecase;

public interface RagQueryUseCase {
  /** Queries knowledge using the tenant bound to the authenticated AI execution context. */
  String query(String question);
}
