package com.emme.identity.api.result;

import java.util.Set;
import java.util.UUID;

/** Tenant membership view included in the consolidated current-user result. */
public record CurrentUserMembershipDetails(
    UUID tenantId,
    String tenantSlug,
    String tenantName,
    String role,
    String status,
    Set<String> permissions) {

  public CurrentUserMembershipDetails {
    permissions = Set.copyOf(permissions);
  }
}
