package com.emme.e2eprovisioner.keycloak;

import java.util.Objects;
import java.util.UUID;

/** Immutable input required to provision the disposable E2E realm. */
public record RealmConfiguration(
    String username, String password, UUID tenantId, String tenantSlug, String webOrigin) {

  public RealmConfiguration {
    Objects.requireNonNull(username, "username must not be null");
    Objects.requireNonNull(password, "password must not be null");
    Objects.requireNonNull(tenantId, "tenantId must not be null");
    Objects.requireNonNull(tenantSlug, "tenantSlug must not be null");
    Objects.requireNonNull(webOrigin, "webOrigin must not be null");
  }
}
