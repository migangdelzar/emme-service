package com.emme.studio.adapter.out.persistence.entity;

import com.emme.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "artist_capability",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"artist_id", "service_id"})})
public class ArtistCapabilityEntity extends TenantOwnedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "artist_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_artist_capability_artist"))
  private ArtistEntity artist;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "service_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_artist_capability_service"))
  private ServiceEntity service;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  protected ArtistCapabilityEntity() {}

  public ArtistCapabilityEntity(UUID tenantId, ArtistEntity artist, ServiceEntity service) {
    super(tenantId);
    this.artist = Objects.requireNonNull(artist, "artist must not be null");
    this.service = Objects.requireNonNull(service, "service must not be null");
  }

  public ArtistEntity getArtist() {
    return artist;
  }

  public ServiceEntity getService() {
    return service;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
