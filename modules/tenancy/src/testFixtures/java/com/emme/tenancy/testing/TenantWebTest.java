package com.emme.tenancy.testing;

import com.emme.tenancy.api.command.CreateTenantCommand;
import com.emme.tenancy.api.result.TenantDetails;
import com.emme.tenancy.api.usecase.CreateTenantUseCase;
import com.emme.testing.BaseWebTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/** Web-test fixture for controller tests that need to provision a tenant. */
@Import(com.emme.testing.TestBootstrapJdbcConfig.class)
public abstract class TenantWebTest extends BaseWebTest {

  @Autowired protected CreateTenantUseCase createTenantUseCase;

  protected TenantDetails createTenant(String slug, String name) {
    return createTenantUseCase.create(new CreateTenantCommand(slug, name));
  }
}
