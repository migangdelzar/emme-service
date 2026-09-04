package com.emme.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.api.query.GetCurrentUserQuery;
import com.emme.identity.api.result.CurrentUserDetails;
import com.emme.identity.api.result.MembershipDetails;
import com.emme.identity.api.usecase.GetCurrentUserMembershipsUseCase;
import com.emme.identity.api.usecase.GetUserPermissionsUseCase;
import com.emme.tenancy.api.result.TenantDetails;
import com.emme.tenancy.api.usecase.GetTenantUseCase;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetCurrentUserServiceTest {

  @Test
  void assemblesCurrentUserDataThroughApplicationCapabilities() {
    UUID tenantId = UUID.randomUUID();
    GetCurrentUserMembershipsUseCase memberships =
        query ->
            List.of(
                new MembershipDetails(
                    UUID.randomUUID(), tenantId, "Tenant", "tenant_owner", "ACTIVE"));
    GetUserPermissionsUseCase permissions =
        (userReference, requestedTenantId) -> Set.of("tenant:read");
    GetTenantUseCase tenants =
        query ->
            Optional.of(
                new TenantDetails(
                    tenantId,
                    "tenant",
                    "Tenant",
                    "tenant_schema",
                    "ACTIVE",
                    "DEDICATED",
                    "emme-tenant"));

    GetCurrentUserService service = new GetCurrentUserService(memberships, permissions, tenants);

    CurrentUserDetails result =
        service.get(new GetCurrentUserQuery("user-1", "user@example.com", "User", tenantId));

    assertThat(result.userId()).isEqualTo("user-1");
    assertThat(result.memberships()).hasSize(1);
    assertThat(result.memberships().getFirst().permissions()).containsExactly("tenant:read");
    assertThat(result.profile()).isNull();
  }
}
