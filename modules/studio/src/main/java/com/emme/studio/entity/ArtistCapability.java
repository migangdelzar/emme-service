package com.emme.studio.entity;

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
public class ArtistCapability extends TenantOwnedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "artist_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_artist_capability_artist"))
  private Artist artist;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "service_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_artist_capability_service"))
  private Service service;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  protected ArtistCapability() {}

  public ArtistCapability(UUID tenantId, Artist artist, Service service) {
    super(tenantId);
    this.artist = Objects.requireNonNull(artist, "artist must not be null");
    this.service = Objects.requireNonNull(service, "service must not be null");
  }

  public Artist getArtist() {
    return artist;
  }

  public Service getService() {
    return service;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
