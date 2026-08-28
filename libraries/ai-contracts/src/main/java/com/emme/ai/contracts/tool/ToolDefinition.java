package com.emme.ai.contracts.tool;

import java.util.Objects;
import java.util.Set;

/** Registered tool metadata; handlers and domain rules remain outside the contract library. */
public record ToolDefinition(
    String key, String description, Set<String> argumentNames, ToolPolicy policy) {

  public ToolDefinition {
    key = requireText(key, "key");
    description = requireText(description, "description");
    Objects.requireNonNull(argumentNames, "argumentNames must not be null");
    if (argumentNames.stream().anyMatch(argument -> argument == null || argument.isBlank())) {
      throw new IllegalArgumentException("argumentNames must not contain blank values");
    }
    argumentNames = Set.copyOf(argumentNames);
    policy = Objects.requireNonNull(policy, "policy must not be null");
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
