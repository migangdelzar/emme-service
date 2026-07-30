package com.emme.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.kernel.context.TenantContext;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.kernel.tracing.CorrelationContextHolder;
import com.emme.kernel.tracing.CorrelationId;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class TenantContextHolderTest {

  @AfterEach
  void tearDown() {
    TenantContext.clear();
    CorrelationId.clear();
    MDC.clear();
  }

  @Test
  void currentTenantOptionalReturnsCurrentTenantWhenPresent() {
    UUID tenantId = UUID.randomUUID();
    TenantContext.setCurrentTenant(tenantId);

    assertThat(TenantContextHolder.currentTenantOptional()).contains(tenantId);
  }

  @Test
  void currentDatabaseOptionalReturnsCurrentDatabaseWhenPresent() {
    UUID databaseId = UUID.randomUUID();
    TenantContext.setCurrentDatabaseId(databaseId);

    assertThat(TenantContextHolder.currentDatabaseOptional()).contains(databaseId);
  }

  @Test
  void withTenantAndCorrelationSetsBothContextsAndRestoresPreviousValues() {
    UUID previousTenantId = UUID.randomUUID();
    UUID previousDatabaseId = UUID.randomUUID();
    UUID nextTenantId = UUID.randomUUID();
    UUID nextDatabaseId = UUID.randomUUID();

    TenantContext.setCurrentTenant(previousTenantId);
    TenantContext.setCurrentDatabaseId(previousDatabaseId);
    MDC.put("tenantId", previousTenantId.toString());

    CorrelationContextHolder.withCorrelationId(
        "previous",
        () -> {
          TenantContextHolder.withTenantAndCorrelation(
              nextTenantId,
              nextDatabaseId,
              "next",
              () -> {
                assertThat(TenantContextHolder.requireCurrentTenantId()).isEqualTo(nextTenantId);
                assertThat(TenantContextHolder.currentDatabaseOptional()).contains(nextDatabaseId);
                assertThat(CorrelationContextHolder.requireCorrelationId()).isEqualTo("next");
                assertThat(MDC.get("tenantId")).isEqualTo(nextTenantId.toString());
                return null;
              });

          assertThat(TenantContextHolder.requireCurrentTenantId()).isEqualTo(previousTenantId);
          assertThat(TenantContextHolder.currentDatabaseOptional()).contains(previousDatabaseId);
          assertThat(CorrelationContextHolder.requireCorrelationId()).isEqualTo("previous");
          assertThat(MDC.get("tenantId")).isEqualTo(previousTenantId.toString());
          return null;
        });
  }
}
