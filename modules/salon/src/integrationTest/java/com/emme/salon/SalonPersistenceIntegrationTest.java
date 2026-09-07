package com.emme.salon;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.salon.adapter.out.persistence.entity.BusinessProfileEntity;
import com.emme.salon.adapter.out.persistence.repository.SpringDataBusinessProfileRepository;
import com.emme.tenancy.adapter.out.client.database.TenantSchemaName;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
class SalonPersistenceIntegrationTest {

  @Autowired private SpringDataBusinessProfileRepository profileRepository;
  @Autowired private TenantProvisioningRepository provisioningRepository;
  @Autowired private TenantSchemaMigrationPort schemaMigrationPort;

  @Autowired
  @Qualifier("bootstrapJdbcClient")
  private JdbcClient bootstrapJdbc;

  @Test
  void businessProfileSingletonUsesTheTenantSelectedSchema() {
    bootstrapJdbc.sql("CREATE EXTENSION IF NOT EXISTS vector").update();
    UUID tenantA = provisionTenant("salon-a");
    UUID tenantB = provisionTenant("salon-b");

    TenantContextHolder.withTenantOverride(
        tenantA,
        () ->
            profileRepository.save(
                new BusinessProfileEntity(tenantA, "America/Mexico_City", "es-MX", "Salon A")));

    TenantContextHolder.withTenantOverride(
        tenantB, () -> assertThat(profileRepository.findFirstByOrderByCreatedAtAsc()).isEmpty());

    TenantContextHolder.withTenantOverride(
        tenantA,
        () ->
            assertThat(profileRepository.findFirstByOrderByCreatedAtAsc())
                .get()
                .extracting(BusinessProfileEntity::getDisplayName)
                .isEqualTo("Salon A"));
  }

  private UUID provisionTenant(String prefix) {
    UUID tenantId = UUID.randomUUID();
    String slug = prefix + "-" + tenantId.toString().replace('-', 'a');
    provisioningRepository.requestProvisioning(tenantId, slug, TenantSchemaName.fromSlug(slug));
    assertThat(schemaMigrationPort.migrate(tenantId, slug))
        .isEqualTo(TenantSchemaName.fromSlug(slug));
    return tenantId;
  }
}
