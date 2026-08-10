package com.emme.identity.application.service;

import com.emme.identity.api.command.AuthenticateUserCommand;
import com.emme.identity.api.query.GetUserClaimsQuery;
import com.emme.identity.api.result.UserClaimsResult;
import com.emme.identity.api.result.UserTokenResult;
import com.emme.identity.api.usecase.AuthenticateUserUseCase;
import com.emme.identity.application.port.out.IdentityRealmConfigurationPort;
import com.emme.identity.application.port.out.UserAuthenticationPort;
import com.emme.tenancy.api.query.ListTenantsQuery;
import com.emme.tenancy.api.usecase.ListTenantsUseCase;
import org.springframework.stereotype.Service;

/** Coordinates user authentication and tenant-realm selection. */
@Service
public class AuthenticateUserService implements AuthenticateUserUseCase {

  private final UserAuthenticationPort authenticationPort;
  private final ListTenantsUseCase listTenants;
  private final IdentityRealmConfigurationPort realmConfiguration;

  public AuthenticateUserService(
      UserAuthenticationPort authenticationPort,
      ListTenantsUseCase listTenants,
      IdentityRealmConfigurationPort realmConfiguration) {
    this.authenticationPort = authenticationPort;
    this.listTenants = listTenants;
    this.realmConfiguration = realmConfiguration;
  }

  @Override
  public UserTokenResult authenticate(AuthenticateUserCommand command) {
    return authenticationPort.authenticate(
        resolveRealm(command.username()), command.username(), command.password());
  }

  @Override
  public UserClaimsResult getUserClaims(GetUserClaimsQuery query) {
    return authenticationPort.getUserClaims(query.accessToken());
  }

  private String resolveRealm(String email) {
    if (email.endsWith("@emme.app")
        && !email.contains("@demo-salon")
        && !email.contains("@studio-a")) {
      return realmConfiguration.defaultRealm();
    }

    String domain = email.substring(email.indexOf('@') + 1);
    return listTenants.list(new ListTenantsQuery()).stream()
        .filter(tenant -> domain.contains(tenant.slug()))
        .map(tenant -> tenant.identityRealm())
        .findFirst()
        .orElse(realmConfiguration.defaultRealm());
  }
}
