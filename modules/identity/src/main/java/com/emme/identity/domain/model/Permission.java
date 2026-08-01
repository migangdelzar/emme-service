package com.emme.identity.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Framework-free Identity permission model. */
public final class Permission {

  private final UUID id;
  private final String code;
  private final String name;
  private final String description;
  private boolean active;
  private final Instant createdAt;
  private final Instant updatedAt;

  public Permission(String code, String name, String description) {
    this(null, code, name, description, true, null, null);
  }

  private Permission(
      UUID id,
      String code,
      String name,
      String description,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.code = Objects.requireNonNull(code, "code must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.description = description;
    this.active = active;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static Permission rehydrate(
      UUID id,
      String code,
      String name,
      String description,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {
    return new Permission(
        Objects.requireNonNull(id, "id must not be null"),
        code,
        name,
        description,
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

  public String description() {
    return description;
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
