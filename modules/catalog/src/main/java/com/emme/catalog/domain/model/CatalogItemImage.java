package com.emme.catalog.domain.model;

import com.emme.shared.identity.IdGenerator;
import java.util.Objects;
import java.util.UUID;

/** Pure catalog image reference. Storage and database mapping are owned by the outbound adapter. */
public class CatalogItemImage {

  private final UUID id;
  private final UUID tenantId;
  private UUID catalogItemId;
  private String storageKey;
  private String caption;

  public CatalogItemImage(UUID tenantId, UUID catalogItemId, String storageKey, String caption) {
    this(IdGenerator.generate(), tenantId, catalogItemId, storageKey, caption);
  }

  public CatalogItemImage(
      UUID id, UUID tenantId, UUID catalogItemId, String storageKey, String caption) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    this.catalogItemId = Objects.requireNonNull(catalogItemId, "catalogItemId must not be null");
    this.storageKey = Objects.requireNonNull(storageKey, "storageKey must not be null");
    this.caption = caption;
  }

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getCatalogItemId() {
    return catalogItemId;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public String getCaption() {
    return caption;
  }
}
