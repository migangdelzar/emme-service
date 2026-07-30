package com.emme.identity.entity;

import com.emme.shared.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "role_permission",
    schema = "emme_core",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"role_id", "permission_id"})})
public class RolePermission extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "role_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_role_permission_role"))
  private Role role;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "permission_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_role_permission_permission"))
  private Permission permission;

  protected RolePermission() {}

  public RolePermission(Role role, Permission permission) {
    this.role = role;
    this.permission = permission;
  }

  public Role getRole() {
    return role;
  }

  public Permission getPermission() {
    return permission;
  }
}
