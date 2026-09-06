package com.emme.identity.api.result;

import com.emme.identity.api.type.MembershipStatus;
import java.time.Instant;
import java.util.UUID;

/** Public membership read model returned by Identity use cases. */
public record MembershipDetails(
    UUID id,
    UUID tenantId,
    String tenantName,
    String roleCode,
    String userReference,
    MembershipStatus status,
    Instant createdAt) {

  public MembershipDetails(
      UUID id, UUID tenantId, String tenantName, String roleCode, MembershipStatus status) {
    this(id, tenantId, tenantName, roleCode, null, status, null);
  }
}
