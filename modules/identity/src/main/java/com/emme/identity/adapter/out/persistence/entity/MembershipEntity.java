package com.emme.identity.adapter.out.persistence.entity;

import com.emme.identity.domain.model.MembershipStatus;
import com.emme.shared.persistence.PersistedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

/** JPA representation of the Identity membership aggregate. */
@Entity
@Table(name = "membership", schema = "emme_core")
public class MembershipEntity extends PersistedEntity {

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "role_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_membership_role"))
  private RoleEntity role;

  @Column(name = "user_reference", nullable = false, length = 150)
  private String userReference;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private MembershipStatus status = MembershipStatus.ACTIVE;

  protected MembershipEntity() {}

  public MembershipEntity(UUID tenantId, RoleEntity role, String userReference) {
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    this.role = Objects.requireNonNull(role, "role must not be null");
    this.userReference = Objects.requireNonNull(userReference, "userReference must not be null");
  }

  public static MembershipEntity restore(
      UUID id,
      UUID tenantId,
      RoleEntity role,
      String userReference,
      MembershipStatus status,
      java.time.Instant createdAt,
      java.time.Instant updatedAt) {
    MembershipEntity entity = new MembershipEntity(tenantId, role, userReference);
    entity.status = Objects.requireNonNull(status, "status must not be null");
    entity.restoreAuditFields(id, createdAt, updatedAt);
    return entity;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public RoleEntity getRole() {
    return role;
  }

  public String getUserReference() {
    return userReference;
  }

  public MembershipStatus getStatus() {
    return status;
  }

  public void setStatus(MembershipStatus status) {
    this.status = Objects.requireNonNull(status, "status must not be null");
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
