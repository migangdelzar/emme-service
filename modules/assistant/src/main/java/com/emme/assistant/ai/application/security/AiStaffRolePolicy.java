package com.emme.assistant.ai.application.security;

import java.util.Set;

/** Shared application policy for roles allowed to review or resume staff workflows. */
public final class AiStaffRolePolicy {

  private static final Set<String> STAFF_ROLES =
      Set.of(
          "tenant_staff",
          "tenant_owner",
          "ROLE_tenant_staff",
          "ROLE_tenant_owner",
          "ROLE_STAFF",
          "ROLE_OWNER",
          "ROLE_ADMIN",
          "ROLE_admin",
          "admin");

  private AiStaffRolePolicy() {}

  public static boolean isStaff(Set<String> roles) {
    return roles != null && roles.stream().anyMatch(STAFF_ROLES::contains);
  }
}
