package com.emme.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Base MappedSuperclass providing UUIDv7 id and timestamp columns for all JPA entities in the
 * system.
 */
@MappedSuperclass
public abstract class BaseEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private Long version;

  public UUID getId() {
    return id;
  }

  /**
   * Assigns an identifier while mapping a domain object to its persistence representation.
   * Persistence adapters are the only callers; normal entities receive identifiers in {@link
   * #onCreate()}.
   */
  protected void setId(UUID id) {
    this.id = Objects.requireNonNull(id, "id must not be null");
  }

  /** Restores persisted identity and audit fields when mapping a domain aggregate to an entity. */
  protected void restoreAuditFields(UUID id, Instant createdAt, Instant updatedAt) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  @PrePersist
  public void onCreate() {
    if (id == null) {
      id = IdGenerator.generate();
    }
    Instant now = ClockProvider.instant();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  public void onUpdate() {
    updatedAt = ClockProvider.instant();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BaseEntity that)) return false;
    return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : super.hashCode();
  }
}
