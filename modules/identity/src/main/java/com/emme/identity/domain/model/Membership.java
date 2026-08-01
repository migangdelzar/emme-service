package com.emme.identity.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Framework-free membership aggregate linking a user, tenant, and role. */
public final class Membership {

  private final UUID id;
  private final UUID tenantId;
  private final UUID roleId;
  private final String roleCode;
  private final String userReference;
  private MembershipStatus status;
  private final Instant createdAt;
  private final Instant updatedAt;

  public Membership(UUID tenantId, UUID roleId, String roleCode, String userReference) {
    this(null, tenantId, roleId, roleCode, userReference, MembershipStatus.ACTIVE, null, null);
  }

  private Membership(
      UUID id,
      UUID tenantId,
      UUID roleId,
      String roleCode,
      String userReference,
      MembershipStatus status,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    this.roleId = Objects.requireNonNull(roleId, "roleId must not be null");
    this.roleCode = Objects.requireNonNull(roleCode, "roleCode must not be null");
    this.userReference = Objects.requireNonNull(userReference, "userReference must not be null");
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static Membership rehydrate(
      UUID id,
      UUID tenantId,
      UUID roleId,
      String roleCode,
      String userReference,
      MembershipStatus status,
      Instant createdAt,
      Instant updatedAt) {
    return new Membership(
        Objects.requireNonNull(id, "id must not be null"),
        tenantId,
        roleId,
        roleCode,
        userReference,
        status,
        Objects.requireNonNull(createdAt, "createdAt must not be null"),
        Objects.requireNonNull(updatedAt, "updatedAt must not be null"));
  }

  public UUID id() {
    return id;
  }

  public UUID tenantId() {
    return tenantId;
  }

  public UUID roleId() {
    return roleId;
  }

  public String roleCode() {
    return roleCode;
  }

  public String userReference() {
    return userReference;
  }

  public MembershipStatus status() {
    return status;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public void suspend() {
    this.status = MembershipStatus.SUSPENDED;
  }

  public void reactivate() {
    this.status = MembershipStatus.ACTIVE;
  }

  public void revoke() {
    this.status = MembershipStatus.REVOKED;
  }
}
