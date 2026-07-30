package com.emme.catalog.domain.model;

import com.emme.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

/**
 * Reference photo of a catalog item's expected result. caption is the gemma3:4b vision description;
 * embedding/caption_tsv are unmapped (see CatalogItem docs).
 */
@Entity
@Table(name = "catalog_item_image")
public class CatalogItemImage extends TenantOwnedEntity {

  @Column(name = "catalog_item_id", nullable = false)
  private UUID catalogItemId;

  @Column(name = "storage_key", nullable = false, length = 500)
  private String storageKey;

  @Column(name = "caption", length = 2000)
  private String caption;

  protected CatalogItemImage() {}

  public CatalogItemImage(UUID tenantId, UUID catalogItemId, String storageKey, String caption) {
    super(tenantId);
    this.catalogItemId = Objects.requireNonNull(catalogItemId, "catalogItemId must not be null");
    this.storageKey = Objects.requireNonNull(storageKey, "storageKey must not be null");
    this.caption = caption;
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
