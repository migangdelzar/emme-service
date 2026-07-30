package com.emme.catalog.domain.model;

import com.emme.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A priced, sellable subtype of a Service (e.g. "Francés clásico" under "Uñas acrílicas"). The
 * embedding vector and search_tsv columns exist in Postgres but are intentionally NOT mapped here —
 * vector reads/writes go through com.emme.shared.search.HybridSearch (native SQL, Postgres-only).
 */
@Entity
@Table(
    name = "catalog_item",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant_id", "service_id", "code"})})
public class CatalogItem extends TenantOwnedEntity {

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
  private CatalogItemStatus status = CatalogItemStatus.ACTIVE;

  protected CatalogItem() {}

  public CatalogItem(
      UUID tenantId,
      UUID serviceId,
      String code,
      String name,
      String description,
      BigDecimal price,
      String priceNotes,
      Integer durationMinutes,
      String materials) {
    super(tenantId);
    this.serviceId = Objects.requireNonNull(serviceId, "serviceId must not be null");
    this.code = Objects.requireNonNull(code, "code must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.description = description;
    this.price = Objects.requireNonNull(price, "price must not be null");
    this.priceNotes = priceNotes;
    this.durationMinutes = durationMinutes;
    this.materials = materials;
  }

  public UUID getServiceId() {
    return serviceId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public String getPriceNotes() {
    return priceNotes;
  }

  public Integer getDurationMinutes() {
    return durationMinutes;
  }

  public String getMaterials() {
    return materials;
  }

  public CatalogItemStatus getStatus() {
    return status;
  }

  public void setStatus(CatalogItemStatus status) {
    this.status = status;
  }

  /** Text that gets embedded for semantic matching. */
  public String embeddingText() {
    StringBuilder sb = new StringBuilder(name);
    if (description != null) sb.append(' ').append(description);
    if (materials != null) sb.append(' ').append(materials);
    return sb.toString();
  }
}
