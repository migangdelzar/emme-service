package com.emme.kernel.context;

import com.emme.functional.unchecked.UFunction;
import com.emme.functional.unchecked.URunnable;
import com.emme.functional.unchecked.USupplier;
import com.emme.kernel.tracing.CorrelationContextHolder;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;

public final class TenantContextHolder {

  private static final String MDC_TENANT_KEY = "tenantId";

  private TenantContextHolder() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static UUID requireCurrentTenantId() {
    UUID tenantId = TenantContext.getCurrentTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("No tenant context");
    }
    return tenantId;
  }

  public static Optional<UUID> currentTenantOptional() {
    return Optional.ofNullable(TenantContext.getCurrentTenantId());
  }

  public static Optional<UUID> currentDatabaseOptional() {
    return Optional.ofNullable(TenantContext.getCurrentDatabaseId());
  }

  public static <T> T withCurrentTenant(UFunction<UUID, T> action) {
    return action.apply(requireCurrentTenantId());
  }

  public static <T> T withTenantOverride(UUID tenantId, USupplier<T> action) {
    return withTenantOverride(tenantId, null, action);
  }

  public static void withTenantOverride(UUID tenantId, URunnable action) {
    withTenantOverride(
        tenantId,
        () -> {
          action.runThrows();
          return null;
        });
  }

  public static <T> T withTenantOverride(UUID tenantId, UUID databaseId, USupplier<T> action) {
    if (tenantId == null) {
      throw new IllegalArgumentException("tenantId must not be null");
    }

    UUID previousTenantId = TenantContext.getCurrentTenantId();
    UUID previousDatabaseId = TenantContext.getCurrentDatabaseId();
    String previousMdcTenantId = MDC.get(MDC_TENANT_KEY);
    try {
      TenantContext.setCurrentTenant(tenantId);
      TenantContext.setCurrentDatabaseId(databaseId);
      MDC.put(MDC_TENANT_KEY, tenantId.toString());
      return action.get();
    } finally {
      restore(previousTenantId, previousDatabaseId, previousMdcTenantId);
    }
  }

  public static void withTenantOverride(UUID tenantId, UUID databaseId, URunnable action) {
    withTenantOverride(
        tenantId,
        databaseId,
        () -> {
          action.runThrows();
          return null;
        });
  }

  public static <T> T withTenantAndCorrelation(
      UUID tenantId, String correlationId, USupplier<T> action) {
    return withTenantAndCorrelation(tenantId, null, correlationId, action);
  }

  public static void withTenantAndCorrelation(
      UUID tenantId, String correlationId, URunnable action) {
    withTenantAndCorrelation(tenantId, null, correlationId, action);
  }

  public static <T> T withTenantAndCorrelation(
      UUID tenantId, UUID databaseId, String correlationId, USupplier<T> action) {
    return withTenantOverride(
        tenantId,
        databaseId,
        () -> CorrelationContextHolder.withCorrelationId(correlationId, action));
  }

  public static void withTenantAndCorrelation(
      UUID tenantId, UUID databaseId, String correlationId, URunnable action) {
    withTenantAndCorrelation(
        tenantId,
        databaseId,
        correlationId,
        () -> {
          action.runThrows();
          return null;
        });
  }

  private static void restore(UUID tenantId, UUID databaseId, String mdcTenantId) {
    TenantContext.clear();
    if (tenantId != null) {
      TenantContext.setCurrentTenant(tenantId);
    }
    TenantContext.setCurrentDatabaseId(databaseId);

    if (mdcTenantId == null) {
      MDC.remove(MDC_TENANT_KEY);
    } else {
      MDC.put(MDC_TENANT_KEY, mdcTenantId);
    }
  }
}
