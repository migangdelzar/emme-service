package com.emme.identity.api.result;

import java.time.Instant;
import java.util.UUID;

/** Public membership read model returned by Identity use cases. */
public record MembershipInfo(
    UUID id,
    UUID tenantId,
    String tenantName,
    String roleCode,
    String userReference,
    String status,
    Instant createdAt) {

  public MembershipInfo(UUID id, UUID tenantId, String tenantName, String roleCode, String status) {
    this(id, tenantId, tenantName, roleCode, null, status, null);
  }
}
