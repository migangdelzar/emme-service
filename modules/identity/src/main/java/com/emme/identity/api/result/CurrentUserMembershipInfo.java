package com.emme.identity.api.result;

import java.util.Set;
import java.util.UUID;

/** Tenant membership view included in the consolidated current-user result. */
public record CurrentUserMembershipInfo(
    UUID tenantId,
    String tenantSlug,
    String tenantName,
    String role,
    String status,
    Set<String> permissions) {

  public CurrentUserMembershipInfo {
    permissions = Set.copyOf(permissions);
  }
}
