package com.emme.clients;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.clients.adapter.out.persistence.entity.CustomerEntity;
import com.emme.clients.adapter.out.persistence.repository.SpringDataCustomerRepository;
import com.emme.kernel.context.TenantContextHolder;
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
class CustomerPersistenceIntegrationTest {

  @Autowired private SpringDataCustomerRepository customerRepository;
  @Autowired private TenantProvisioningRepository provisioningRepository;
  @Autowired private TenantSchemaMigrationPort schemaMigrationPort;

  @Autowired
  @Qualifier("bootstrapJdbcClient")
  private JdbcClient bootstrapJdbc;

  @Test
  void customerCrudUsesTheTenantSelectedSchema() {
    bootstrapJdbc.sql("CREATE EXTENSION IF NOT EXISTS vector").update();
    UUID tenantA = provisionTenant("customer-a");
    UUID tenantB = provisionTenant("customer-b");

    UUID customerId =
        TenantContextHolder.withTenantOverride(
            tenantA,
            () -> customerRepository.save(new CustomerEntity(tenantA, "Tenant A")).getId());

    TenantContextHolder.withTenantOverride(
        tenantB, () -> assertThat(customerRepository.findById(customerId)).isEmpty());

    TenantContextHolder.withTenantOverride(
        tenantA,
        () ->
            assertThat(customerRepository.findById(customerId))
                .get()
                .extracting(CustomerEntity::getName)
                .isEqualTo("Tenant A"));
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
