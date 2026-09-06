package com.emme.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emme.identity.api.command.AuthenticateUserCommand;
import com.emme.identity.api.result.UserTokenResult;
import com.emme.identity.application.port.out.IdentityRealmConfigurationPort;
import com.emme.identity.application.port.out.UserAuthenticationPort;
import com.emme.tenancy.api.query.ListTenantsQuery;
import com.emme.tenancy.api.result.TenantDetails;
import com.emme.tenancy.api.type.TenantStatus;
import com.emme.tenancy.api.usecase.ListTenantsUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {

  @Mock private UserAuthenticationPort authenticationPort;
  @Mock private ListTenantsUseCase listTenants;

  @Test
  void resolvesPlatformUsersToTheConfiguredDefaultRealm() {
    IdentityRealmConfigurationPort realmConfiguration = () -> "emme";
    when(authenticationPort.authenticate("emme", "admin@emme.app", "secret"))
        .thenReturn(new UserTokenResult("access", "refresh", "id"));

    var result =
        new AuthenticateUserService(authenticationPort, listTenants, realmConfiguration)
            .authenticate(new AuthenticateUserCommand("admin@emme.app", "secret"));

    assertThat(result.accessToken()).isEqualTo("access");
  }

  @Test
  void resolvesTenantUsersFromTheirEmailDomain() {
    IdentityRealmConfigurationPort realmConfiguration = () -> "emme";
    when(listTenants.list(new ListTenantsQuery()))
        .thenReturn(
            List.of(
                new TenantDetails(
                    null,
                    "demo-salon",
                    "Demo",
                    "schema",
                    TenantStatus.ACTIVE,
                    "DEDICATED",
                    "emme-demo")));
    when(authenticationPort.authenticate("emme-demo", "owner@demo-salon.test", "secret"))
        .thenReturn(new UserTokenResult("access", null, null));

    var result =
        new AuthenticateUserService(authenticationPort, listTenants, realmConfiguration)
            .authenticate(new AuthenticateUserCommand("owner@demo-salon.test", "secret"));

    assertThat(result.accessToken()).isEqualTo("access");
  }
}
