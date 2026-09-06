package com.emme.tenancy.adapter.out.persistence.entity;

import com.emme.shared.time.ClockProvider;
import com.emme.tenancy.domain.model.TenantProvisioningState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_registry", schema = "emme_core")
public class TenantRegistryEntity {

  @Id
  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "slug", nullable = false, unique = true, length = 63)
  private String slug;

  @Column(name = "schema_name", nullable = false, unique = true, length = 63)
  private String schemaName;

  @Column(name = "database_mode", nullable = false, length = 16)
  private String databaseMode = "SHARED";

  @Column(name = "database_key", nullable = false, length = 128)
  private String databaseKey = "emme";

  @Column(name = "status", nullable = false, length = 24)
  @Enumerated(EnumType.STRING)
  private TenantProvisioningState status = TenantProvisioningState.PROVISIONING;

  @Column(name = "schema_version", length = 64)
  private String schemaVersion;

  @Column(name = "last_migrated_at")
  private Instant lastMigratedAt;

  @Column(name = "migration_error")
  private String migrationError;

  @Column(name = "database_id")
  private UUID databaseId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected TenantRegistryEntity() {}

  public TenantRegistryEntity(
      UUID tenantId, String slug, String schemaName, TenantProvisioningState status) {
    this.tenantId = tenantId;
    this.slug = slug;
    this.schemaName = schemaName;
    this.status = status;
  }

  @PrePersist
  void onCreate() {
    var now = ClockProvider.instant();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = ClockProvider.instant();
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getSlug() {
    return slug;
  }

  public String getSchemaName() {
    return schemaName;
  }

  public TenantProvisioningState getStatus() {
    return status;
  }

  public String getSchemaVersion() {
    return schemaVersion;
  }

  public String getMigrationError() {
    return migrationError;
  }

  public Instant getLastMigratedAt() {
    return lastMigratedAt;
  }

  public UUID getDatabaseId() {
    return databaseId;
  }

  public void setStatus(TenantProvisioningState status) {
    this.status = status;
  }

  public void setSchemaVersion(String schemaVersion) {
    this.schemaVersion = schemaVersion;
  }

  public void setLastMigratedAt(Instant lastMigratedAt) {
    this.lastMigratedAt = lastMigratedAt;
  }

  public void setMigrationError(String migrationError) {
    this.migrationError = migrationError;
  }
}
