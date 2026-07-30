package com.emme.tenancy.entity;

import com.emme.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tenant", schema = "emme_core")
public class Tenant extends BaseEntity {

  @Column(name = "slug", nullable = false, unique = true, length = 50)
  private String slug;

  @Column(name = "name", nullable = false, length = 150)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private TenantStatus status = TenantStatus.ACTIVE;

  @Column(name = "database_id")
  private UUID databaseId;

  @Column(name = "keycloak_realm", nullable = false, length = 100)
  private String keycloakRealm = "emme";

  protected Tenant() {}

  public Tenant(String slug, String name) {
    this.slug = Objects.requireNonNull(slug, "slug must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.status = TenantStatus.ACTIVE;
  }

  public String getSlug() {
    return slug;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public TenantStatus getStatus() {
    return status;
  }

  public void suspend() {
    this.status = TenantStatus.SUSPENDED;
  }

  public void reactivate() {
    this.status = TenantStatus.ACTIVE;
  }

  public void markDeleted() {
    this.status = TenantStatus.DELETED;
  }

  public UUID getDatabaseId() {
    return databaseId;
  }

  public void setDatabaseId(UUID databaseId) {
    this.databaseId = databaseId;
  }

  public String getKeycloakRealm() {
    return keycloakRealm;
  }

  public void setKeycloakRealm(String keycloakRealm) {
    this.keycloakRealm = keycloakRealm;
  }
}
