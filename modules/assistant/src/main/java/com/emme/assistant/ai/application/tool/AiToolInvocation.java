package com.emme.assistant.ai.application.tool;

import java.util.Map;

/** Model-independent tool invocation request. Confirmation flags are backend-controlled inputs. */
public record AiToolInvocation(
    String toolKey, Map<String, String> arguments, boolean userConfirmed, boolean staffApproved) {

  public AiToolInvocation {
    requireText(toolKey, "toolKey");
    if (arguments == null) throw new NullPointerException("arguments must not be null");
    arguments.forEach(
        (key, value) -> {
          requireText(key, "argument name");
          if (value == null) throw new IllegalArgumentException("argument value must not be null");
        });
    arguments = Map.copyOf(arguments);
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
