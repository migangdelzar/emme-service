package com.emme.services.adapter.out.persistence.entity;

import com.emme.shared.persistence.TenantOwnedEntity;
import com.emme.services.domain.model.ArtistStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "artist")
public class ArtistEntity extends TenantOwnedEntity {

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 10)
  private ArtistStatus status = ArtistStatus.ACTIVE;

  protected ArtistEntity() {}

  public ArtistEntity(UUID tenantId, String name) {
    super(tenantId);
    this.name = Objects.requireNonNull(name, "name must not be null");
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ArtistStatus getStatus() {
    return status;
  }

  public void setStatus(ArtistStatus status) {
    this.status = status;
  }
}
