package com.emme.identity.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Framework-free Identity role model. */
public final class Role {

  private final UUID id;
  private final String code;
  private final String name;
  private final RoleScope scope;
  private boolean active;
  private final Instant createdAt;
  private final Instant updatedAt;

  public Role(String code, String name, RoleScope scope) {
    this(null, code, name, scope, true, null, null);
  }

  private Role(
      UUID id,
      String code,
      String name,
      RoleScope scope,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.code = Objects.requireNonNull(code, "code must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.scope = Objects.requireNonNull(scope, "scope must not be null");
    this.active = active;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static Role rehydrate(
      UUID id,
      String code,
      String name,
      RoleScope scope,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {
    return new Role(
        Objects.requireNonNull(id, "id must not be null"),
        code,
        name,
        scope,
        active,
        Objects.requireNonNull(createdAt, "createdAt must not be null"),
        Objects.requireNonNull(updatedAt, "updatedAt must not be null"));
  }

  public UUID id() {
    return id;
  }

  public String code() {
    return code;
  }

  public String name() {
    return name;
  }

  public RoleScope scope() {
    return scope;
  }

  public boolean isActive() {
    return active;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public void activate() {
    this.active = true;
  }

  public void deactivate() {
    this.active = false;
  }
}
