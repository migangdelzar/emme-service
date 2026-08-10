package com.emme.tenancy.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "database_registry", schema = "emme_core")
public class DatabaseRegistry {

  @Id
  @Column(name = "database_id", nullable = false, updatable = false)
  private UUID databaseId;

  @Column(name = "name", nullable = false, unique = true, length = 100)
  private String name;

  @Column(name = "jdbc_url", nullable = false, length = 500)
  private String jdbcUrl;

  @Column(name = "min_pool_size")
  private Integer minPoolSize;

  @Column(name = "max_pool_size")
  private Integer maxPoolSize;

  @Column(name = "priority")
  private Integer priority;

  @Column(name = "is_active")
  private Boolean isActive;

  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  /** Public for the bootstrap registry adapter that hydrates rows without JPA. */
  public DatabaseRegistry() {}

  public DatabaseRegistry(
      String name, String jdbcUrl, Integer minPoolSize, Integer maxPoolSize, Integer priority) {
    this.databaseId = UUID.randomUUID();
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl must not be null");
    this.minPoolSize = minPoolSize != null ? minPoolSize : 5;
    this.maxPoolSize = maxPoolSize != null ? maxPoolSize : 20;
    this.priority = priority != null ? priority : 0;
    this.isActive = true;
    this.createdAt = Instant.now();
  }

  public UUID getDatabaseId() {
    return databaseId;
  }

  public void setDatabaseId(UUID databaseId) {
    this.databaseId = databaseId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getJdbcUrl() {
    return jdbcUrl;
  }

  public void setJdbcUrl(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
  }

  public Integer getMinPoolSize() {
    return minPoolSize;
  }

  public void setMinPoolSize(Integer minPoolSize) {
    this.minPoolSize = minPoolSize;
  }

  public Integer getMaxPoolSize() {
    return maxPoolSize;
  }

  public void setMaxPoolSize(Integer maxPoolSize) {
    this.maxPoolSize = maxPoolSize;
  }

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DatabaseRegistry that)) return false;
    return databaseId != null && Objects.equals(databaseId, that.databaseId);
  }

  @Override
  public int hashCode() {
    return databaseId != null ? databaseId.hashCode() : super.hashCode();
  }
}
