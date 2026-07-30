package com.emme.studio.entity;

import com.emme.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "artist")
public class Artist extends TenantOwnedEntity {

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 10)
  private ArtistStatus status = ArtistStatus.ACTIVE;

  protected Artist() {}

  public Artist(UUID tenantId, String name) {
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
