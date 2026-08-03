package com.emme.studio.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Domain representation of an artist working for a Studio tenant. */
public final class Artist {

  private final UUID id;
  private final UUID tenantId;
  private String name;
  private ArtistStatus status;

  public Artist(UUID tenantId, String name) {
    this(null, tenantId, name, ArtistStatus.ACTIVE);
  }

  private Artist(UUID id, UUID tenantId, String name, ArtistStatus status) {
    this.id = id;
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.status = Objects.requireNonNull(status, "status must not be null");
  }

  public static Artist reconstitute(UUID id, UUID tenantId, String name, ArtistStatus status) {
    return new Artist(id, tenantId, name, status);
  }

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = Objects.requireNonNull(name, "name must not be null");
  }

  public ArtistStatus getStatus() {
    return status;
  }

  public void deactivate() {
    status = ArtistStatus.INACTIVE;
  }
}
