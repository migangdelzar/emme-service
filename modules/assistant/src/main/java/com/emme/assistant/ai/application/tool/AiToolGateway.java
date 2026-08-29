package com.emme.assistant.ai.application.tool;

import java.util.Set;

/** Executes registered AI tools after backend authorization and confirmation checks. */
public interface AiToolGateway {

  Set<String> proactivelyEligibleToolKeys();

  /** Returns only backend-authorized, non-mutating definitions safe for model tool calling. */
  Set<AiToolDefinition> agentEligibleToolDefinitions();

  AiToolResult execute(AiToolInvocation invocation);
}
