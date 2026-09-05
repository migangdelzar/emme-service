package com.emme.catalog.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure catalog business model. Database annotations and tenant persistence concerns belong to
 * {@code adapter.out.persistence.entity}; this type owns the catalog behavior consumed by the
 * application layer.
 */
public class CatalogItem {

  private final UUID id;
  private final UUID tenantId;
  private UUID serviceId;
  private String code;
  private String name;
  private String description;
  private BigDecimal price;
  private String priceNotes;
  private Integer durationMinutes;
  private String materials;
  private CatalogItemStatus status = CatalogItemStatus.ACTIVE;

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
    this(
        null,
        tenantId,
        serviceId,
        code,
        name,
        description,
        price,
        priceNotes,
        durationMinutes,
        materials,
        CatalogItemStatus.ACTIVE);
  }

  public CatalogItem(
      UUID id,
      UUID tenantId,
      UUID serviceId,
      String code,
      String name,
      String description,
      BigDecimal price,
      String priceNotes,
      Integer durationMinutes,
      String materials,
      CatalogItemStatus status) {
    this.id = id;
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    this.serviceId = Objects.requireNonNull(serviceId, "serviceId must not be null");
    this.code = Objects.requireNonNull(code, "code must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.description = description;
    this.price = Objects.requireNonNull(price, "price must not be null");
    this.priceNotes = priceNotes;
    this.durationMinutes = durationMinutes;
    this.materials = materials;
    this.status = Objects.requireNonNull(status, "status must not be null");
  }

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
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

  public void changeStatus(CatalogItemStatus status) {
    this.status = Objects.requireNonNull(status, "status must not be null");
  }

  /** Text that gets embedded for semantic matching. */
  public String embeddingText() {
    StringBuilder sb = new StringBuilder(name);
    if (description != null) sb.append(' ').append(description);
    if (materials != null) sb.append(' ').append(materials);
    return sb.toString();
  }
}
