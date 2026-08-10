package com.emme.notification.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.notification.adapter.out.provider.sms.MessageBirdProvider;
import com.emme.notification.adapter.out.provider.sms.SmsProviderException;
import com.emme.notification.configuration.NotificationHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessageBirdProviderContractTest {
  private MockWebServer server;
  private MessageBirdProvider provider;

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
    provider =
        new MessageBirdProvider(
            new NotificationHttpClient(new OkHttpClient()),
            baseUrl(),
            "key-123",
            "Emme",
            new ObjectMapper());
  }

  @AfterEach
  void tearDown() throws Exception {
    server.shutdown();
  }

  @Test
  void sendsMessageBirdJsonContractAndReturnsProviderId() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(201).setBody("{\"id\":\"mb-123\"}"));

    assertThat(provider.send("+5215551111111", "Hello")).isEqualTo("messagebird-mb-123");

    var request = server.takeRequest();
    assertThat(request.getHeader("Authorization")).isEqualTo("AccessKey key-123");
    assertThat(request.getBody().readUtf8())
        .contains(
            "\"recipients\":[\"+5215551111111\"]", "\"originator\":\"Emme\"", "\"body\":\"Hello\"");
  }

  @Test
  void translatesProviderHttpFailureToTypedException() {
    server.enqueue(new MockResponse().setResponseCode(503).setBody("unavailable"));

    assertThatThrownBy(() -> provider.send("+5215551111111", "Hello"))
        .isInstanceOf(SmsProviderException.class)
        .hasMessage("MessageBird send failed: HTTP 503");
  }

  private String baseUrl() {
    return server.url("/").toString().replaceFirst("/$", "");
  }
}
