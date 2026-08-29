package com.emme.assistant.ai.application.tool;

import java.util.Objects;
import java.util.Set;

/** Typed policy and handler metadata for one backend-controlled AI tool. */
public record AiToolDefinition(
    String key,
    String description,
    Set<String> allowedRoles,
    AiToolRisk risk,
    boolean userConfirmationRequired,
    boolean staffApprovalRequired,
    AiToolHandler handler,
    Set<String> requiredArgumentNames,
    Set<String> allowedArgumentNames) {

  public AiToolDefinition(
      String key,
      String description,
      Set<String> allowedRoles,
      AiToolRisk risk,
      boolean userConfirmationRequired,
      boolean staffApprovalRequired,
      AiToolHandler handler) {
    this(
        key,
        description,
        allowedRoles,
        risk,
        userConfirmationRequired,
        staffApprovalRequired,
        handler,
        Set.of(),
        Set.of());
  }

  public AiToolDefinition {
    requireText(key, "key");
    requireText(description, "description");
    Objects.requireNonNull(allowedRoles, "allowedRoles must not be null");
    if (allowedRoles.isEmpty()) {
      throw new IllegalArgumentException("allowedRoles must not be empty");
    }
    allowedRoles =
        allowedRoles.stream()
            .map(role -> canonicalRole(requireText(role, "allowedRole")))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    Objects.requireNonNull(risk, "risk must not be null");
    Objects.requireNonNull(handler, "handler must not be null");
    Objects.requireNonNull(requiredArgumentNames, "requiredArgumentNames must not be null");
    Objects.requireNonNull(allowedArgumentNames, "allowedArgumentNames must not be null");
    requiredArgumentNames = normalizeArgumentNames(requiredArgumentNames, "required argument");
    allowedArgumentNames = normalizeArgumentNames(allowedArgumentNames, "allowed argument");
    if (!allowedArgumentNames.isEmpty()
        && !allowedArgumentNames.containsAll(requiredArgumentNames)) {
      throw new IllegalArgumentException("required arguments must be allowed arguments");
    }
  }

  public boolean isAuthorized(Set<String> roles) {
    if (roles == null || roles.isEmpty()) return false;
    return roles.stream().anyMatch(role -> allowedRoles.contains(canonicalRole(role)));
  }

  public boolean canRunProactively() {
    return risk == AiToolRisk.READ_ONLY
        && !userConfirmationRequired
        && !staffApprovalRequired
        && requiredArgumentNames.isEmpty();
  }

  private static Set<String> normalizeArgumentNames(Set<String> names, String field) {
    return names.stream()
        .map(name -> requireText(name, field))
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }

  private static String canonicalRole(String role) {
    if (role == null || role.isBlank()) return "";
    return role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role;
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
