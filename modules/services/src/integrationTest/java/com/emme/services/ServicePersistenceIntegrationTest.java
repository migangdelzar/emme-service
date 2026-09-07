package com.emme.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.services.adapter.out.persistence.entity.ServiceEntity;
import com.emme.services.adapter.out.persistence.repository.SpringDataServiceRepository;
import com.emme.tenancy.adapter.out.client.database.TenantSchemaName;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
class ServicePersistenceIntegrationTest {

  @Autowired private SpringDataServiceRepository serviceRepository;
  @Autowired private TenantProvisioningRepository provisioningRepository;
  @Autowired private TenantSchemaMigrationPort schemaMigrationPort;

  @Autowired
  @Qualifier("bootstrapJdbcClient")
  private JdbcClient bootstrapJdbc;

  @Test
  void serviceCrudUsesTheTenantSelectedSchema() {
    bootstrapJdbc.sql("CREATE EXTENSION IF NOT EXISTS vector").update();
    UUID tenantA = provisionTenant("service-a");
    UUID tenantB = provisionTenant("service-b");

    UUID serviceId =
        TenantContextHolder.withTenantOverride(
            tenantA,
            () ->
                serviceRepository
                    .save(new ServiceEntity(tenantA, "CUT", "Cut", 30, BigDecimal.valueOf(25)))
                    .getId());

    TenantContextHolder.withTenantOverride(
        tenantB, () -> assertThat(serviceRepository.findById(serviceId)).isEmpty());

    TenantContextHolder.withTenantOverride(
        tenantA,
        () ->
            assertThat(serviceRepository.findById(serviceId))
                .get()
                .extracting(ServiceEntity::getName)
                .isEqualTo("Cut"));
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
