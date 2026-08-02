package com.emme.catalog.adapter.out.persistence.entity;

import com.emme.catalog.domain.model.CatalogItemImage;
import com.emme.shared.persistence.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Database representation of a catalog image. Never crosses the catalog adapter boundary. */
@Entity
@Table(name = "catalog_item_image")
public class CatalogItemImageEntity extends TenantOwnedEntity {

  @Column(name = "catalog_item_id", nullable = false)
  private UUID catalogItemId;

  @Column(name = "storage_key", nullable = false, length = 500)
  private String storageKey;

  @Column(name = "caption", length = 2000)
  private String caption;

  protected CatalogItemImageEntity() {}

  private CatalogItemImageEntity(CatalogItemImage image) {
    super(image.getTenantId());
    setId(image.getId());
    this.catalogItemId = image.getCatalogItemId();
    this.storageKey = image.getStorageKey();
    this.caption = image.getCaption();
  }

  public static CatalogItemImageEntity from(CatalogItemImage image) {
    return new CatalogItemImageEntity(image);
  }

  public CatalogItemImage toDomain() {
    return new CatalogItemImage(getId(), getTenantId(), catalogItemId, storageKey, caption);
  }
}
