package com.emme.catalog.adapter.out.persistence.entity;

import com.emme.catalog.domain.model.CatalogItem;
import com.emme.catalog.domain.model.CatalogItemStatus;
import com.emme.shared.persistence.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;

/** Database representation of a catalog item. Never crosses the catalog adapter boundary. */
@Entity
@Table(
    name = "catalog_item",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant_id", "service_id", "code"})})
public class CatalogItemEntity extends TenantOwnedEntity {

  @Column(name = "service_id", nullable = false)
  private UUID serviceId;

  @Column(name = "code", nullable = false, length = 50)
  private String code;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "description", length = 2000)
  private String description;

  @Column(name = "price", nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(name = "price_notes", length = 500)
  private String priceNotes;

  @Column(name = "duration_minutes")
  private Integer durationMinutes;

  @Column(name = "materials")
  private String materials;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 10)
  private CatalogItemStatus status;

  protected CatalogItemEntity() {}

  private CatalogItemEntity(CatalogItem item) {
    super(item.getTenantId());
    if (item.getId() != null) {
      setId(item.getId());
    }
    this.serviceId = item.getServiceId();
    this.code = item.getCode();
    this.name = item.getName();
    this.description = item.getDescription();
    this.price = item.getPrice();
    this.priceNotes = item.getPriceNotes();
    this.durationMinutes = item.getDurationMinutes();
    this.materials = item.getMaterials();
    this.status = item.getStatus();
  }

  public static CatalogItemEntity from(CatalogItem item) {
    return new CatalogItemEntity(item);
  }

  public void setStatus(CatalogItemStatus status) {
    this.status = status;
  }

  public CatalogItem toDomain() {
    return new CatalogItem(
        getId(),
        getTenantId(),
        serviceId,
        code,
        name,
        description,
        price,
        priceNotes,
        durationMinutes,
        materials,
        status);
  }
}
