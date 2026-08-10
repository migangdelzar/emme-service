package com.emme.testing.tenancy.provisioning;

import com.emme.tenancy.api.command.RequestTenantProvisioningCommand;
import com.emme.tenancy.api.usecase.RequestTenantProvisioningUseCase;
import com.emme.testing.tenancy.fixture.TenantFixture;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Provisions tenants using the <strong>real</strong> production {@link
 * RequestTenantProvisioningUseCase} path (Liquibase migrations included).
 *
 * <p>Used by consuming modules (booking, catalog, etc.) to set up test tenants. The tenancy
 * module's own integration tests call the provisioning use case directly — never this helper.
 */
@Component
public class TenantTestProvisioner {

  private final RequestTenantProvisioningUseCase provisioningService;

  public TenantTestProvisioner(RequestTenantProvisioningUseCase provisioningService) {
    this.provisioningService = provisioningService;
  }

  /**
   * Provisions a tenant and returns its UUID.
   *
   * <p>This runs the full provisioning pipeline: database registry write, schema creation,
   * Liquibase migration. Use sparingly — prefer {@link
   * com.emme.testing.tenancy.fixture.TenantFixtures} constants when tenant identity is all that
   * matters.
   */
  public UUID provision(TenantFixture tenant) {
    return provisioningService.request(
        new RequestTenantProvisioningCommand(tenant.slug(), tenant.displayName(), "UTC", "en"));
  }
}
