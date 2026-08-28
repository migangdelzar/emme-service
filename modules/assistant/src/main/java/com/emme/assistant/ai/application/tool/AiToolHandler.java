package com.emme.assistant.ai.application.tool;

import java.util.Map;

/** Executes one validated tool definition through an injected application-use-case adapter. */
@FunctionalInterface
public interface AiToolHandler {

  String execute(AiToolExecutionContext context, Map<String, String> arguments);
}
