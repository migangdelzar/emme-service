package com.emme.assistant.ai.api.command;

import java.util.Map;
import java.util.Objects;

/** Typed client clarification supplied to resume a workflow awaiting required slots. */
public record WorkflowClarificationCommand(String answer, Map<String, String> slots) {

  public WorkflowClarificationCommand {
    if (answer == null || answer.isBlank()) {
      throw new IllegalArgumentException("answer must not be blank");
    }
    slots = Map.copyOf(Objects.requireNonNull(slots, "slots must not be null"));
    if (slots.keySet().stream().anyMatch(key -> key == null || key.isBlank())
        || slots.values().stream().anyMatch(value -> value == null || value.isBlank())) {
      throw new IllegalArgumentException("slots must not contain blank keys or values");
    }
  }
}
