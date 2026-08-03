package com.emme.studio.adapter.out.persistence.entity;

import com.emme.shared.persistence.TenantOwnedEntity;
import com.emme.studio.domain.model.ServiceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "service",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"tenant_id", "code"})})
public class ServiceEntity extends TenantOwnedEntity {

  @Column(name = "code", nullable = false, length = 50)
  private String code;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "category", nullable = false, length = 120)
  private String category;

  @Column(name = "description", length = 1000)
  private String description;

  @Column(name = "duration_minutes", nullable = false)
  private int durationMinutes;

  @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal basePrice;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 10)
  private ServiceStatus status = ServiceStatus.ACTIVE;

  protected ServiceEntity() {}

  public ServiceEntity(
      UUID tenantId, String code, String name, int durationMinutes, BigDecimal basePrice) {
    this(tenantId, code, name, "Servicios Complementarios", null, durationMinutes, basePrice);
  }

  public ServiceEntity(
      UUID tenantId,
      String code,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice) {
    super(tenantId);
    this.code = Objects.requireNonNull(code, "code must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.category = Objects.requireNonNull(category, "category must not be null");
    this.description = description;
    this.durationMinutes = durationMinutes;
    this.basePrice = Objects.requireNonNull(basePrice, "basePrice must not be null");
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
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
    this.basePrice = basePrice;
  }

  public ServiceStatus getStatus() {
    return status;
  }

  public void setStatus(ServiceStatus status) {
    this.status = status;
  }
}
