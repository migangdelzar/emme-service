package com.emme.tenancy.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class TenantProvisioningPersistenceAdapterTest {

  @Test
  void requestsAreIdempotentForTheSameSlugAndSchema() {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    UUID tenantId = UUID.randomUUID();
    when(jdbc.queryForObject(anyString(), eq(UUID.class), eq("studio-a"), eq("studio_a")))
        .thenReturn(tenantId);

    UUID result =
        new TenantProvisioningPersistenceAdapter(jdbc).requestProvisioning("studio-a", "studio_a");

    assertThat(result).isEqualTo(tenantId);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).queryForObject(sql.capture(), eq(UUID.class), eq("studio-a"), eq("studio_a"));
    assertThat(sql.getValue())
        .contains("ON CONFLICT (slug) DO UPDATE")
        .contains("tenant_registry.schema_name = EXCLUDED.schema_name")
        .contains("RETURNING tenant_id");
  }
}
