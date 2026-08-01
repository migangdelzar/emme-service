package com.emme.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emme.identity.api.command.AuthenticateUserCommand;
import com.emme.identity.api.result.UserTokenResult;
import com.emme.identity.application.port.out.UserAuthenticationPort;
import com.emme.identity.configuration.IdentityKeycloakProperties;
import com.emme.tenancy.api.result.TenantInfo;
import com.emme.tenancy.api.usecase.TenantApi;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {

  @Mock private UserAuthenticationPort authenticationPort;
  @Mock private TenantApi tenantApi;

  @Test
  void resolvesPlatformUsersToTheConfiguredDefaultRealm() {
    IdentityKeycloakProperties properties = new IdentityKeycloakProperties();
    properties.setDefaultRealm("emme");
    when(authenticationPort.authenticate("emme", "admin@emme.app", "secret"))
        .thenReturn(new UserTokenResult("access", "refresh", "id"));

    var result =
        new AuthenticateUserService(authenticationPort, tenantApi, properties)
            .authenticate(new AuthenticateUserCommand("admin@emme.app", "secret"));

    assertThat(result.accessToken()).isEqualTo("access");
  }

  @Test
  void resolvesTenantUsersFromTheirEmailDomain() {
    IdentityKeycloakProperties properties = new IdentityKeycloakProperties();
    properties.setDefaultRealm("emme");
    when(tenantApi.getAllTenants())
        .thenReturn(
            List.of(
                new TenantInfo(
                    null, "demo-salon", "Demo", "schema", "ACTIVE", "DEDICATED", "emme-demo")));
    when(authenticationPort.authenticate("emme-demo", "owner@demo-salon.test", "secret"))
        .thenReturn(new UserTokenResult("access", null, null));

    var result =
        new AuthenticateUserService(authenticationPort, tenantApi, properties)
            .authenticate(new AuthenticateUserCommand("owner@demo-salon.test", "secret"));

    assertThat(result.accessToken()).isEqualTo("access");
  }
}
