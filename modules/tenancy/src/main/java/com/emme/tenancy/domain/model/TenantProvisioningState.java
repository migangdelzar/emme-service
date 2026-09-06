package com.emme.tenancy.domain.model;

/** Control-plane lifecycle states for tenant schema and realm provisioning. */
public enum TenantProvisioningState {
  PROVISIONING,
  ACTIVE,
  FAILED
}
