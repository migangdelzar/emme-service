package com.emme.testing.tenancy.context;

import com.emme.kernel.context.TenantContextHolder;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Convenience wrappers around {@link TenantContextHolder#withTenantOverride} that handle the
 * checked-exception wrapper pattern.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * TenantTestContext.withTenant(tenantId, () -> {
 *     var result = repository.findAll();
 *     assertThat(result).hasSize(3);
 * });
 * }</pre>
 */
public final class TenantTestContext {

  private TenantTestContext() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Execute an operation within the given tenant context. The previous tenant is restored after
   * completion (success or failure).
   */
  public static void withTenant(UUID tenantId, Runnable operation) {
    TenantContextHolder.withTenantOverride(tenantId, () -> operation.run());
  }

  /** Execute a supplier within the given tenant context and return its result. */
  public static <T> T withTenant(UUID tenantId, Supplier<T> operation) {
    return TenantContextHolder.withTenantOverride(tenantId, operation::get);
  }
}
