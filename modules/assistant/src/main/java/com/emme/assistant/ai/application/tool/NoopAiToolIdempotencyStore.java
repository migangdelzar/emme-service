package com.emme.assistant.ai.application.tool;

import com.emme.assistant.ai.application.port.out.AiToolIdempotencyStore;
import java.util.Optional;

/** No-op idempotency boundary used when durable infrastructure is not available. */
public enum NoopAiToolIdempotencyStore implements AiToolIdempotencyStore {
  INSTANCE;

  @Override
  public Optional<AiToolResult> find(String operationKey) {
    return Optional.empty();
  }

  @Override
  public boolean claim(String operationKey, String toolKey) {
    return true;
  }

  @Override
  public void complete(String operationKey, AiToolResult result) {
    // Deliberately empty: this implementation is only for isolated or infrastructure-free runs.
  }

  @Override
  public void release(String operationKey) {
    // Deliberately empty: this implementation is only for isolated or infrastructure-free runs.
  }
}
