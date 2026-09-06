package com.emme.identity.adapter.out.client.keycloak;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.configuration.IdentityKeycloakProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class KeycloakRestClientTransportTest {

  @Test
  void sendsPasswordGrantOverTheConfiguredRestClientTransport() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.start();
      server.enqueue(
          new MockResponse().setResponseCode(200).setBody("{\"access_token\":\"access-123\"}"));
      IdentityKeycloakProperties properties = propertiesFor(server);
      KeycloakUserAuthenticationAdapter adapter =
          new KeycloakUserAuthenticationAdapter(
              RestClient.builder().build(), new ObjectMapper(), properties);

      var result = adapter.authenticate("emme-core", "alice", "password");
      RecordedRequest request = server.takeRequest();

      assertThat(result.accessToken()).isEqualTo("access-123");
      assertThat(request.getMethod()).isEqualTo("POST");
      assertThat(request.getPath()).isEqualTo("/realms/emme-core/protocol/openid-connect/token");
      assertThat(request.getBody().readUtf8())
          .contains("grant_type=password")
          .contains("username=alice")
          .contains("client_id=platform-client");
    }
  }

  private static IdentityKeycloakProperties propertiesFor(MockWebServer server) {
    String baseUrl = server.url("/").toString().replaceAll("/$", "");
    return new IdentityKeycloakProperties(
        baseUrl,
        baseUrl + "/realms/emme-core",
        "",
        "client-123",
        "platform-client",
        "master",
        "admin",
        "admin-password",
        "emme-core",
        baseUrl + "/realms/emme-customers",
        "customer-client");
  }
}
