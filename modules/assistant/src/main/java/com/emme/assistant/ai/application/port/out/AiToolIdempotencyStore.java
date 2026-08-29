package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.application.tool.AiToolResult;
import java.util.Optional;

/**
 * Durable command idempotency boundary for mutation tools.
 *
 * <p>The operation key is derived by the backend from the authenticated execution context. Model
 * supplied arguments are deliberately not used as an authority for identity or tenancy.
 */
public interface AiToolIdempotencyStore {

  Optional<AiToolResult> find(String operationKey);

  boolean claim(String operationKey, String toolKey);

  void complete(String operationKey, AiToolResult result);

  void release(String operationKey);
}
