package com.emme.identity.adapter.out.client.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.emme.identity.api.exception.IdentityAuthenticationException;
import com.emme.identity.api.result.UserTokenResult;
import com.emme.identity.configuration.IdentityKeycloakProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

class KeycloakProviderContractTest {

  @Test
  void authenticatesAgainstTheRealmTokenEndpointUsingTheConfiguredClient() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    KeycloakUserAuthenticationAdapter adapter = userAdapter(builder);

    server
        .expect(requestTo("https://keycloak.test/realms/emme-core/protocol/openid-connect/token"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().formData(authenticationForm()))
        .andRespond(
            withStatus(HttpStatus.OK)
                .body("{\"access_token\":\"access-123\",\"refresh_token\":\"refresh-123\"}"));

    UserTokenResult result = adapter.authenticate("emme-core", "alice", "password");

    assertThat(result.accessToken()).isEqualTo("access-123");
    assertThat(result.refreshToken()).isEqualTo("refresh-123");
    server.verify();
  }

  @Test
  void mapsKeycloakAuthenticationRejectionToTheExistingDomainException() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    KeycloakUserAuthenticationAdapter adapter = userAdapter(builder);
    server
        .expect(requestTo("https://keycloak.test/realms/emme-core/protocol/openid-connect/token"))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("invalid credentials"));

    assertThatThrownBy(() -> adapter.authenticate("emme-core", "alice", "wrong"))
        .isInstanceOf(IdentityAuthenticationException.class)
        .hasMessage("Invalid credentials");
    server.verify();
  }

  @Test
  void createsARealmThroughAnAdminTokenAndPreservesTheRealmRepresentation() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    KeycloakAdminClient client = adminClient(builder);

    expectAdminToken(server);
    server
        .expect(requestTo("https://keycloak.test/admin/realms"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer admin-token"))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "realm": "tenant-a",
                      "enabled": true,
                      "displayName": "Tenant A",
                      "accessTokenLifespan": 3600
                    }
                    """))
        .andRespond(withStatus(HttpStatus.CREATED));

    client.createRealm("tenant-a", "Tenant A");
    server.verify();
  }

  @Test
  void createsAUserThenSetsPasswordLooksUpRoleAndAssignsIt() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    KeycloakAdminClient client = adminClient(builder);

    expectAdminToken(server);
    server
        .expect(requestTo("https://keycloak.test/admin/realms/tenant-a/users"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer admin-token"))
        .andRespond(
            withStatus(HttpStatus.CREATED)
                .header("Location", "/admin/realms/tenant-a/users/user-123"));
    server
        .expect(
            requestTo("https://keycloak.test/admin/realms/tenant-a/users/user-123/reset-password"))
        .andExpect(method(HttpMethod.PUT))
        .andRespond(withStatus(HttpStatus.NO_CONTENT));
    server
        .expect(requestTo("https://keycloak.test/admin/realms/tenant-a/roles/staff"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.OK).body("{\"id\":\"role-123\"}"));
    server
        .expect(
            requestTo(
                "https://keycloak.test/admin/realms/tenant-a/users/user-123/role-mappings/realm"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json("[{\"id\":\"role-123\",\"name\":\"staff\"}]"))
        .andRespond(withStatus(HttpStatus.NO_CONTENT));

    assertThat(client.createUser("tenant-a", "alice", "alice@example.com", "password", "staff"))
        .isEqualTo("user-123");
    server.verify();
  }

  private static void expectAdminToken(MockRestServiceServer server) {
    server
        .expect(requestTo("https://keycloak.test/realms/master/protocol/openid-connect/token"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.OK).body("{\"access_token\":\"admin-token\"}"));
  }

  private static KeycloakAdminClient adminClient(RestClient.Builder builder) {
    return new KeycloakAdminClient(properties(), new ObjectMapper(), builder.build());
  }

  private static KeycloakUserAuthenticationAdapter userAdapter(RestClient.Builder builder) {
    return new KeycloakUserAuthenticationAdapter(builder.build(), new ObjectMapper(), properties());
  }

  private static MultiValueMap<String, String> authenticationForm() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", "platform-app");
    form.add("grant_type", "password");
    form.add("username", "alice");
    form.add("password", "password");
    form.add("scope", "openid profile email");
    return form;
  }

  private static IdentityKeycloakProperties properties() {
    return new IdentityKeycloakProperties(
        "https://keycloak.test",
        "https://keycloak.test/realms/emme-core",
        "",
        "client-app",
        "platform-app",
        "master",
        "admin",
        "admin-password",
        "emme-core",
        "https://keycloak.test/realms/emme-customers",
        "customer-app");
  }
}
