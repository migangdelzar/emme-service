package com.emme.calendar.adapter.out.google;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.calendar.adapter.out.google.client.GoogleCalendarClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GoogleRestClientTransportTest {

  @Test
  void sendsServiceAccountTokenAndFreeBusyRequestsOverRealSockets() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.start();
      server.enqueue(
          new MockResponse().setResponseCode(200).setBody("{\"access_token\":\"google-token\"}"));
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody("{\"calendars\":{\"primary\":{\"busy\":[]}}}"));
      String tokenUrl = server.url("/token").toString();
      String freeBusyUrl = server.url("/freebusy").toString();
      GoogleCalendarClient client =
          new GoogleCalendarClient(
              RestClient.builder().build(),
              new ObjectMapper(),
              serviceAccountJson(),
              tokenUrl,
              freeBusyUrl);

      assertThat(client.freeBusy("primary", "2026-09-05T10:00:00Z", "2026-09-05T11:00:00Z"))
          .isEmpty();
      RecordedRequest tokenRequest = server.takeRequest();
      RecordedRequest freeBusyRequest = server.takeRequest();

      assertThat(tokenRequest.getMethod()).isEqualTo("POST");
      assertThat(tokenRequest.getPath()).isEqualTo("/token");
      assertThat(tokenRequest.getBody().readUtf8()).contains("grant_type");
      assertThat(freeBusyRequest.getMethod()).isEqualTo("POST");
      assertThat(freeBusyRequest.getPath()).isEqualTo("/freebusy");
      assertThat(freeBusyRequest.getHeader("Authorization")).isEqualTo("Bearer google-token");
    }
  }

  private static String serviceAccountJson() throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    String pem =
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----";
    return Base64.getEncoder()
        .encodeToString(
            new ObjectMapper()
                .writeValueAsBytes(
                    Map.of("client_email", "calendar@example.test", "private_key", pem)));
  }
}
