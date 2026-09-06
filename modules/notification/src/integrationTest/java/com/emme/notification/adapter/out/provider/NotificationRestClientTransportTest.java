package com.emme.notification.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.notification.adapter.out.provider.sms.TwilioSmsProvider;
import java.util.Base64;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class NotificationRestClientTransportTest {

  @Test
  void sendsTwilioFormAndBasicAuthenticationOverRealSockets() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.start();
      server.enqueue(new MockResponse().setResponseCode(201).setBody("{\"sid\":\"SM-123\"}"));
      TwilioSmsProvider provider =
          new TwilioSmsProvider(
              RestClient.builder().build(),
              server.url("/").toString(),
              "account-123",
              "secret-456",
              "+5215550100");

      assertThat(provider.send("+5215550101", "Appointment reminder")).isEqualTo("twilio-SM-123");
      RecordedRequest request = server.takeRequest();

      assertThat(request.getMethod()).isEqualTo("POST");
      assertThat(request.getPath()).isEqualTo("/Accounts/account-123/Messages.json");
      assertThat(request.getHeader("Authorization"))
          .isEqualTo(
              "Basic " + Base64.getEncoder().encodeToString("account-123:secret-456".getBytes()));
      assertThat(request.getBody().readUtf8()).contains("To=%2B5215550101");
    }
  }
}
