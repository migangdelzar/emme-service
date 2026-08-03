package com.emme.identity.application.service;

import com.emme.identity.api.usecase.GetUserPermissionsUseCase;
import com.emme.identity.application.port.out.PermissionPort;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates tenant-scoped permission resolution through an application port. */
@Service
@Transactional(readOnly = true)
public class GetUserPermissionsService implements GetUserPermissionsUseCase {

  private final PermissionPort permissionPort;

  public GetUserPermissionsService(PermissionPort permissionPort) {
    this.permissionPort = permissionPort;
  }

  @Override
  public Set<String> getPermissions(String userReference, UUID tenantId) {
    return permissionPort.findPermissionCodesForUserInTenant(userReference, tenantId);
  }
}
