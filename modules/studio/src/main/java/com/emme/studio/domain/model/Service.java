package com.emme.studio.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Domain representation of a service offered by a Studio tenant. */
public final class Service {

  private final UUID id;
  private final UUID tenantId;
  private final String code;
  private String name;
  private String category;
  private String description;
  private int durationMinutes;
  private BigDecimal basePrice;
  private ServiceStatus status;

  public Service(
      UUID tenantId, String code, String name, int durationMinutes, BigDecimal basePrice) {
    this(tenantId, code, name, "Servicios Complementarios", null, durationMinutes, basePrice);
  }

  public Service(
      UUID tenantId,
      String code,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice) {
    this(
        null,
        tenantId,
        code,
        name,
        category,
        description,
        durationMinutes,
        basePrice,
        ServiceStatus.ACTIVE);
  }

  private Service(
      UUID id,
      UUID tenantId,
      String code,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice,
      ServiceStatus status) {
    this.id = id;
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    this.code = Objects.requireNonNull(code, "code must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.category = Objects.requireNonNull(category, "category must not be null");
    this.description = description;
    this.durationMinutes = durationMinutes;
    this.basePrice = Objects.requireNonNull(basePrice, "basePrice must not be null");
    this.status = Objects.requireNonNull(status, "status must not be null");
  }

  public static Service reconstitute(
      UUID id,
      UUID tenantId,
      String code,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice,
      ServiceStatus status) {
    return new Service(
        id, tenantId, code, name, category, description, durationMinutes, basePrice, status);
  }

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = Objects.requireNonNull(name, "name must not be null");
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = Objects.requireNonNull(category, "category must not be null");
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getDurationMinutes() {
    return durationMinutes;
  }

  public void setDurationMinutes(int durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public BigDecimal getBasePrice() {
    return basePrice;
  }

  public void setBasePrice(BigDecimal basePrice) {
    this.basePrice = Objects.requireNonNull(basePrice, "basePrice must not be null");
  }

  public ServiceStatus getStatus() {
    return status;
  }

  public void retire() {
    status = ServiceStatus.RETIRED;
  }
}
