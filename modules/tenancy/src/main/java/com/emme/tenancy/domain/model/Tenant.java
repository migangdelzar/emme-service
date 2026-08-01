package com.emme.tenancy.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Framework-free tenant aggregate and lifecycle boundary. */
public final class Tenant {

  private final UUID id;
  private final String slug;
  private String name;
  private TenantStatus status;
  private UUID databaseId;
  private String keycloakRealm;
  private final Instant createdAt;
  private final Instant updatedAt;

  public Tenant(String slug, String name) {
    this(null, slug, name, TenantStatus.ACTIVE, null, "emme", null, null);
  }

  private Tenant(
      UUID id,
      String slug,
      String name,
      TenantStatus status,
      UUID databaseId,
      String keycloakRealm,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.slug = Objects.requireNonNull(slug, "slug must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.status = Objects.requireNonNull(status, "status must not be null");
    this.databaseId = databaseId;
    this.keycloakRealm = Objects.requireNonNull(keycloakRealm, "keycloakRealm must not be null");
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static Tenant rehydrate(
      UUID id,
      String slug,
      String name,
      TenantStatus status,
      UUID databaseId,
      String keycloakRealm,
      Instant createdAt,
      Instant updatedAt) {
    return new Tenant(
        Objects.requireNonNull(id, "id must not be null"),
        slug,
        name,
        status,
        databaseId,
        keycloakRealm,
        Objects.requireNonNull(createdAt, "createdAt must not be null"),
        Objects.requireNonNull(updatedAt, "updatedAt must not be null"));
  }

  public UUID id() {
    return id;
  }

  public String slug() {
    return slug;
  }

  public String name() {
    return name;
  }

  public TenantStatus status() {
    return status;
  }

  public UUID databaseId() {
    return databaseId;
  }

  public String keycloakRealm() {
    return keycloakRealm;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public void rename(String name) {
    this.name = Objects.requireNonNull(name, "name must not be null");
  }

  public void changeIdentityRealm(String identityRealm) {
    this.keycloakRealm = Objects.requireNonNull(identityRealm, "identityRealm must not be null");
  }

  public void suspend() {
    requireStatus(TenantStatus.ACTIVE, "suspend");
    this.status = TenantStatus.SUSPENDED;
  }

  public void reactivate() {
    requireStatus(TenantStatus.SUSPENDED, "reactivate");
    this.status = TenantStatus.ACTIVE;
  }

  public void markDeleted() {
    this.status = TenantStatus.DELETED;
  }

  private void requireStatus(TenantStatus expected, String operation) {
    if (status != expected) {
      throw new IllegalStateException("Cannot " + operation + " tenant with status: " + status);
    }
  }
}
