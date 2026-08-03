package com.emme.notification.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.notification.adapter.out.provider.sms.SmsProviderException;
import com.emme.notification.adapter.out.provider.sms.VonageProvider;
import com.emme.notification.configuration.NotificationHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VonageProviderContractTest {
  private MockWebServer server;
  private VonageProvider provider;

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
    provider =
        new VonageProvider(
            new NotificationHttpClient(new OkHttpClient()),
            baseUrl(),
            "key-123",
            "secret-123",
            "Emme",
            new ObjectMapper());
  }

  @AfterEach
  void tearDown() throws Exception {
    server.shutdown();
  }

  @Test
  void sendsVonageJsonContractAndReturnsProviderId() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody("{\"messages\":[{\"message-id\":\"vonage-123\"}]}"));

    assertThat(provider.send("+5215551111111", "Hello")).isEqualTo("vonage-vonage-123");

    var request = server.takeRequest();
    assertThat(request.getBody().readUtf8())
        .contains(
            "\"api_key\":\"key-123\"",
            "\"api_secret\":\"secret-123\"",
            "\"from\":\"Emme\"",
            "\"to\":\"+5215551111111\"",
            "\"text\":\"Hello\"");
  }

  @Test
  void translatesProviderHttpFailureToTypedException() {
    server.enqueue(new MockResponse().setResponseCode(429).setBody("rate limited"));

    assertThatThrownBy(() -> provider.send("+5215551111111", "Hello"))
        .isInstanceOf(SmsProviderException.class)
        .hasMessage("Vonage send failed: HTTP 429");
  }

  private String baseUrl() {
    return server.url("/").toString().replaceFirst("/$", "");
  }
}
