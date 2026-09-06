package com.emme.calendar.adapter.out.google;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.emme.calendar.adapter.out.google.adapter.GoogleOAuthAdapter;
import com.emme.calendar.adapter.out.google.client.GoogleSheetsClient;
import com.emme.kernel.context.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GoogleSheetsProviderContractTest {

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createsASpreadsheetThroughTheBearerAuthenticatedGoogleClient() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    GoogleOAuthAdapter oauth = mock(GoogleOAuthAdapter.class);
    when(oauth.getValidAccessToken(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq("user-123"),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn("google-token");
    GoogleSheetsClient client = new GoogleSheetsClient(oauth, new ObjectMapper(), builder.build());
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("user-123", "credentials"));

    server
        .expect(requestTo("https://sheets.googleapis.com/v4/spreadsheets"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer google-token"))
        .andExpect(content().json("{\"properties\":{\"title\":\"Appointments\"}}"))
        .andRespond(
            withStatus(HttpStatus.OK)
                .body(
                    "{\"spreadsheetId\":\"sheet-123\",\"spreadsheetUrl\":\"https://sheet.test/123\"}"));

    var details =
        TenantContextHolder.withTenantOverride(
            UUID.fromString("00000000-0000-0000-0000-000000000123"),
            () -> client.createSpreadsheet("Appointments"));

    org.assertj.core.api.Assertions.assertThat(details.id()).isEqualTo("sheet-123");
    server.verify();
  }
}
