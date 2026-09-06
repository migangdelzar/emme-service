package com.emme.calendar.adapter.out.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.emme.calendar.adapter.out.google.client.GoogleCalendarClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GoogleCalendarProviderContractTest {

  @Test
  void exchangesAServiceAccountTokenBeforeQueryingGoogleFreeBusy() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    GoogleCalendarClient client = client(builder);
    server
        .expect(requestTo("https://google-oauth.test/token"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentTypeCompatibleWith("application/x-www-form-urlencoded"))
        .andExpect(
            content()
                .string(
                    containsString(
                        "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer")))
        .andRespond(withStatus(HttpStatus.OK).body("{\"access_token\":\"google-token\"}"));
    server
        .expect(requestTo("https://google-calendar.test/freebusy"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer google-token"))
        .andExpect(
            content()
                .json(
                    "{\"timeMin\":\"2026-09-05T10:00:00Z\",\"timeMax\":\"2026-09-05T11:00:00Z\",\"items\":[{\"id\":\"primary\"}]}"))
        .andRespond(withStatus(HttpStatus.OK).body("{\"calendars\":{\"primary\":{\"busy\":[]}}}"));

    assertThat(client.freeBusy("primary", "2026-09-05T10:00:00Z", "2026-09-05T11:00:00Z"))
        .isEmpty();
    server.verify();
  }

  private static GoogleCalendarClient client(RestClient.Builder builder) throws Exception {
    return new GoogleCalendarClient(
        builder.build(),
        new ObjectMapper(),
        serviceAccountJson(),
        "https://google-oauth.test/token",
        "https://google-calendar.test/freebusy");
  }

  private static String serviceAccountJson() throws Exception {
    var keyPair = KeyPairGenerator.getInstance("RSA");
    keyPair.initialize(2048);
    String pem =
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(keyPair.generateKeyPair().getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----";
    return Base64.getEncoder()
        .encodeToString(
            new ObjectMapper()
                .writeValueAsBytes(
                    Map.of("client_email", "calendar@example.test", "private_key", pem)));
  }
}
