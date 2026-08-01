package com.emme.studio.adapter.out.persistence.entity;

import com.emme.shared.TenantOwnedEntity;
import com.emme.studio.domain.model.CustomerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "customer")
public class CustomerEntity extends TenantOwnedEntity {

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "phone", length = 30)
  private String phone;

  @Column(name = "email", length = 200)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 10)
  private CustomerStatus status = CustomerStatus.ACTIVE;

  protected CustomerEntity() {}

  public CustomerEntity(UUID tenantId, String name) {
    super(tenantId);
    this.name = Objects.requireNonNull(name, "name must not be null");
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public void setStatus(CustomerStatus status) {
    this.status = status;
  }
}
