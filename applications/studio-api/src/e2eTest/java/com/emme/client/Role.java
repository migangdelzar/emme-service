package com.emme.client;

/** Keycloak realm roles — matches infra/keycloak/emme-realm.json */
public enum Role {
  PLATFORM_ADMIN,
  BUSINESS_OWNER,
  BUSINESS_MANAGER,
  FRONT_DESK,
  NAIL_ARTIST,
  ACCOUNTANT,
  READ_ONLY
}
