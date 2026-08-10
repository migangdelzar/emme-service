package com.emme.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.application.port.out.PermissionPort;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetUserPermissionsServiceTest {

  @Test
  void returnsPermissionCodesForTheRequestedUserAndTenant() {
    UUID tenantId = UUID.randomUUID();
    PermissionPort permissions = (userReference, requestedTenantId) -> Set.of("quote.read");
    GetUserPermissionsService service = new GetUserPermissionsService(permissions);

    Set<String> result = service.getPermissions("user-123", tenantId);

    assertThat(result).containsExactly("quote.read");
  }

  @Test
  void returnsAnEmptySetWhenTheUserHasNoPermissions() {
    PermissionPort permissions = (userReference, tenantId) -> Set.of();
    GetUserPermissionsService service = new GetUserPermissionsService(permissions);

    Set<String> result = service.getPermissions("user-123", UUID.randomUUID());

    assertThat(result).isEmpty();
  }
}
