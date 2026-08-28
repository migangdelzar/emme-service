package com.emme.ai.contracts.routing;

import com.emme.ai.contracts.tool.ToolRisk;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Catalog definition used for deterministic and semantic intent routing. */
public record IntentDefinition(
    String key,
    String description,
    List<String> examples,
    String toolKey,
    Set<String> requiredSlots,
    Set<String> allowedRoles,
    ToolRisk risk,
    boolean userConfirmationRequired,
    boolean staffApprovalRequired) {

  public IntentDefinition {
    key = requireText(key, "key");
    description = requireText(description, "description");
    examples = immutableTextList(examples, "examples");
    toolKey = requireText(toolKey, "toolKey");
    requiredSlots = immutableTextSet(requiredSlots, "requiredSlots");
    allowedRoles = immutableTextSet(allowedRoles, "allowedRoles");
    if (allowedRoles.isEmpty()) {
      throw new IllegalArgumentException("allowedRoles must not be empty");
    }
    risk = Objects.requireNonNull(risk, "risk must not be null");
  }

  private static List<String> immutableTextList(List<String> values, String field) {
    Objects.requireNonNull(values, field + " must not be null");
    if (values.stream().anyMatch(value -> value == null || value.isBlank())) {
      throw new IllegalArgumentException(field + " must not contain blank values");
    }
    return List.copyOf(values);
  }

  private static Set<String> immutableTextSet(Set<String> values, String field) {
    Objects.requireNonNull(values, field + " must not be null");
    if (values.stream().anyMatch(value -> value == null || value.isBlank())) {
      throw new IllegalArgumentException(field + " must not contain blank values");
    }
    return Set.copyOf(values);
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
