package com.emme.identity.entity;

import com.emme.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "permission", schema = "emme_core")
public class Permission extends BaseEntity {

  @Column(name = "code", nullable = false, unique = true, length = 100)
  private String code;

  @Column(name = "name", nullable = false, length = 150)
  private String name;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  protected Permission() {}

  public Permission(String code, String name, String description) {
    this.code = Objects.requireNonNull(code, "code must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.description = description;
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

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
