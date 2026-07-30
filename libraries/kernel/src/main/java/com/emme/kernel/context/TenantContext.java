package com.emme.kernel.context;

import java.util.UUID;

/**
 * Thread-local holder for the current tenant ID and database ID. Set by the TenantContextFilter
 * after trusted resolution. Cleared at the end of each request.
 */
public final class TenantContext {

  private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();
  private static final ThreadLocal<UUID> CURRENT_DATABASE = new ThreadLocal<>();

  private TenantContext() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static void setCurrentTenant(UUID tenantId) {
    if (tenantId == null) {
      throw new IllegalArgumentException("tenantId must not be null");
    }
    CURRENT_TENANT.set(tenantId);
  }

  public static UUID getCurrentTenantId() {
    return CURRENT_TENANT.get();
  }

  public static void setCurrentDatabaseId(UUID databaseId) {
    CURRENT_DATABASE.set(databaseId);
  }

  public static UUID getCurrentDatabaseId() {
    return CURRENT_DATABASE.get();
  }

  public static void clear() {
    CURRENT_TENANT.remove();
    CURRENT_DATABASE.remove();
  }
}
