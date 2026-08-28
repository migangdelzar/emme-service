package com.emme.assistant.ai.application.tool;

import java.util.Set;

/** Executes registered AI tools after backend authorization and confirmation checks. */
public interface AiToolGateway {

  Set<String> proactivelyEligibleToolKeys();

  AiToolResult execute(AiToolInvocation invocation);
}
