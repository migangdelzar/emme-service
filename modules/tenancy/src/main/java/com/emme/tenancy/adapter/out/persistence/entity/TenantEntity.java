package com.emme.tenancy.adapter.out.persistence.entity;

import com.emme.shared.BaseEntity;
import com.emme.tenancy.domain.model.TenantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence representation of the Tenancy tenant aggregate. */
@Entity
@Table(name = "tenant", schema = "emme_core")
public class TenantEntity extends BaseEntity {

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

  public TenantEntity() {}

  public static TenantEntity restore(
      UUID id,
      String slug,
      String name,
      TenantStatus status,
      UUID databaseId,
      String keycloakRealm,
      Instant createdAt,
      Instant updatedAt) {
    TenantEntity entity = new TenantEntity();
    entity.slug = slug;
    entity.name = name;
    entity.status = status;
    entity.databaseId = databaseId;
    entity.keycloakRealm = keycloakRealm;
    entity.restoreAuditFields(id, createdAt, updatedAt);
    return entity;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
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

  public void setStatus(TenantStatus status) {
    this.status = status;
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
