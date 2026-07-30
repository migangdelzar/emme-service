package com.emme.client;

/** Role constants for @Authenticated annotation. Use instead of magic strings. */
public final class Roles {
  private Roles() {}

  public static final String PLATFORM_ADMIN = "platform_admin";
  public static final String TENANT_OWNER = "tenant_owner";
  public static final String TENANT_STAFF = "tenant_staff";
  public static final String BUSINESS_OWNER = "business_owner";
}
