package com.emme.identity.adapter.out.persistence.entity;

import com.emme.shared.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** JPA representation of the role-to-permission association. */
@Entity
@Table(
    name = "role_permission",
    schema = "emme_core",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"role_id", "permission_id"})})
public class RolePermissionEntity extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "role_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_role_permission_role"))
  private RoleEntity role;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "permission_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_role_permission_permission"))
  private PermissionEntity permission;

  protected RolePermissionEntity() {}

  public RolePermissionEntity(RoleEntity role, PermissionEntity permission) {
    this.role = role;
    this.permission = permission;
  }

  public RoleEntity getRole() {
    return role;
  }

  public PermissionEntity getPermission() {
    return permission;
  }
}
