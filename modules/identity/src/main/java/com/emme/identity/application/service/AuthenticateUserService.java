package com.emme.identity.application.service;

import com.emme.identity.api.command.AuthenticateUserCommand;
import com.emme.identity.api.query.GetUserInfoQuery;
import com.emme.identity.api.result.UserInfoResult;
import com.emme.identity.api.result.UserTokenResult;
import com.emme.identity.api.usecase.AuthenticateUserUseCase;
import com.emme.identity.application.port.out.IdentityRealmConfigurationPort;
import com.emme.identity.application.port.out.UserAuthenticationPort;
import com.emme.tenancy.api.usecase.TenantApi;
import org.springframework.stereotype.Service;

/** Coordinates user authentication and tenant-realm selection. */
@Service
public class AuthenticateUserService implements AuthenticateUserUseCase {

  private final UserAuthenticationPort authenticationPort;
  private final TenantApi tenantApi;
  private final IdentityRealmConfigurationPort realmConfiguration;

  public AuthenticateUserService(
      UserAuthenticationPort authenticationPort,
      TenantApi tenantApi,
      IdentityRealmConfigurationPort realmConfiguration) {
    this.authenticationPort = authenticationPort;
    this.tenantApi = tenantApi;
    this.realmConfiguration = realmConfiguration;
  }

  @Override
  public UserTokenResult authenticate(AuthenticateUserCommand command) {
    return authenticationPort.authenticate(
        resolveRealm(command.username()), command.username(), command.password());
  }

  @Override
  public UserInfoResult getUserInfo(GetUserInfoQuery query) {
    return authenticationPort.getUserInfo(query.accessToken());
  }

  private String resolveRealm(String email) {
    if (email.endsWith("@emme.app")
        && !email.contains("@demo-salon")
        && !email.contains("@studio-a")) {
      return realmConfiguration.defaultRealm();
    }

    String domain = email.substring(email.indexOf('@') + 1);
    return tenantApi.getAllTenants().stream()
        .filter(tenant -> domain.contains(tenant.slug()))
        .map(tenant -> tenant.identityRealm())
        .findFirst()
        .orElse(realmConfiguration.defaultRealm());
  }
}
