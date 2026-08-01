package com.emme.identity.adapter.out.persistence.entity;

import com.emme.identity.domain.model.RoleScope;
import com.emme.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** JPA representation of an Identity role. */
@Entity
@Table(name = "role", schema = "emme_core")
public class RoleEntity extends BaseEntity {

  @Column(name = "code", nullable = false, unique = true, length = 50)
  private String code;

  @Column(name = "name", nullable = false, length = 150)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "scope", nullable = false, length = 10)
  private RoleScope scope = RoleScope.TENANT;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  protected RoleEntity() {}

  public RoleEntity(String code, String name, RoleScope scope) {
    this.code = Objects.requireNonNull(code, "code must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.scope = Objects.requireNonNull(scope, "scope must not be null");
  }

  public static RoleEntity restore(
      UUID id,
      String code,
      String name,
      RoleScope scope,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {
    RoleEntity entity = new RoleEntity(code, name, scope);
    entity.setActive(active);
    entity.restoreAuditFields(id, createdAt, updatedAt);
    return entity;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public RoleScope getScope() {
    return scope;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
