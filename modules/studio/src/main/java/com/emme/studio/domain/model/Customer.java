package com.emme.studio.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Domain representation of a Studio customer. */
public final class Customer {

  private final UUID id;
  private final UUID tenantId;
  private String name;
  private String phone;
  private String email;
  private CustomerStatus status;

  public Customer(UUID tenantId, String name) {
    this(null, tenantId, name, null, null, CustomerStatus.ACTIVE);
  }

  private Customer(
      UUID id, UUID tenantId, String name, String phone, String email, CustomerStatus status) {
    this.id = id;
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.phone = phone;
    this.email = email;
    this.status = Objects.requireNonNull(status, "status must not be null");
  }

  public static Customer reconstitute(
      UUID id, UUID tenantId, String name, String phone, String email, CustomerStatus status) {
    return new Customer(id, tenantId, name, phone, email, status);
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

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public CustomerStatus getStatus() {
    return status;
  }

  public void retire() {
    status = CustomerStatus.RETIRED;
  }
}
