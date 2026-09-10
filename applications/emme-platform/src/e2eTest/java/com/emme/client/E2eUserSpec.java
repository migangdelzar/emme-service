package com.emme.client;

import java.util.List;

/** Describes one E2E identity without coupling a test to token environment variable names. */
public record E2eUserSpec(
    List<String> roles, String tenantId, String accessToken, boolean authenticated) {

  public E2eUserSpec {
    roles = roles == null ? List.of() : List.copyOf(roles);
    tenantId = tenantId == null ? "" : tenantId;
    accessToken = accessToken == null ? "" : accessToken;
  }

  /** Creates an authenticated identity requiring all supplied roles. */
  public static E2eUserSpec authenticated(String... roles) {
    return new E2eUserSpec(List.of(roles), "", "", true);
  }

  /** Creates an authenticated identity with an explicitly supplied bearer token. */
  public static E2eUserSpec authenticatedWithToken(String accessToken, String... roles) {
    return new E2eUserSpec(List.of(roles), "", accessToken, true);
  }

  /** Creates an unauthenticated identity. */
  public static E2eUserSpec unauthenticated() {
    return new E2eUserSpec(List.of(), "", "", false);
  }
}
