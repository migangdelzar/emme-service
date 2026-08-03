package com.emme.notification.adapter.out.provider.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.notification.configuration.NotificationHttpClient;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TwilioSmsProviderContractTest {
  private MockWebServer server;
  private TwilioSmsProvider provider;

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
    provider =
        new TwilioSmsProvider(
            new NotificationHttpClient(new OkHttpClient()),
            baseUrl(),
            "AC123",
            "token123",
            "+5215550000000");
  }

  @AfterEach
  void tearDown() throws Exception {
    server.shutdown();
  }

  @Test
  void sendsSmsUsingTwilioBasicAuthenticationAndFormContract() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(201).setBody("{\"sid\":\"SM123\"}"));

    assertThat(provider.send("+5215551111111", "Hello")).isEqualTo("twilio-SM123");
    var request = server.takeRequest();
    assertThat(request.getHeader("Authorization")).startsWith("Basic ");
    assertThat(request.getBody().readUtf8())
        .contains("To=%2B5215551111111", "From=%2B5215550000000", "Body=Hello");
  }

  @Test
  void throwsTypedProviderFailureWhenTwilioRejectsTheMessage() {
    server.enqueue(new MockResponse().setResponseCode(429).setBody("rate limited"));

    assertThatThrownBy(() -> provider.send("+5215551111111", "Hello"))
        .isInstanceOf(SmsProviderException.class)
        .hasMessage("Twilio send failed: HTTP 429");
  }

  private String baseUrl() {
    return server.url("/").toString().replaceFirst("/$", "");
  }
}
