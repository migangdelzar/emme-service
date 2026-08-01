package com.emme.studio.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Associates an artist with a service they can perform. */
public final class ArtistCapability {

  private final UUID id;
  private final UUID tenantId;
  private final Artist artist;
  private final Service service;
  private boolean active;

  public ArtistCapability(UUID tenantId, Artist artist, Service service) {
    this(null, tenantId, artist, service, true);
  }

  private ArtistCapability(UUID id, UUID tenantId, Artist artist, Service service, boolean active) {
    this.id = id;
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    this.artist = Objects.requireNonNull(artist, "artist must not be null");
    this.service = Objects.requireNonNull(service, "service must not be null");
    this.active = active;
  }

  public static ArtistCapability reconstitute(
      UUID id, UUID tenantId, Artist artist, Service service, boolean active) {
    return new ArtistCapability(id, tenantId, artist, service, active);
  }

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
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

  public void deactivate() {
    active = false;
  }
}
