package com.emme.payment.adapter.out.client.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.payment.application.port.out.PaymentProviderException;
import com.emme.payment.configuration.PaymentHttpClient;
import com.emme.payment.configuration.PaymentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StripeProviderContractTest {
  private MockWebServer server;
  private StripeProvider provider;

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
    provider =
        new StripeProvider(
            properties(),
            new PaymentHttpClient(new OkHttpClient()),
            server.url("/").toString(),
            new ObjectMapper());
  }

  @AfterEach
  void tearDown() throws Exception {
    server.shutdown();
  }

  @Test
  void initiatesPaymentWithStripeAuthenticationAndIdempotencyContract() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody("{\"id\":\"pi_123\",\"client_secret\":\"secret_123\"}"));

    var result = provider.initiate("request-123", new BigDecimal("12.50"), "MXN", "Premium");

    assertThat(result.providerTransactionId()).isEqualTo("pi_123");
    assertThat(result.status()).isEqualTo("PENDING");
    assertThat(result.metadata()).containsEntry("client_secret", "secret_123");
    var request = server.takeRequest();
    assertThat(request.getHeader("Authorization")).isEqualTo("Bearer sk_test");
    assertThat(request.getHeader("Idempotency-Key")).isEqualTo("request-123");
    assertThat(request.getBody().readUtf8()).contains("amount=1250", "currency=mxn");
  }

  @Test
  void mapsProviderFailureToPaymentProviderException() {
    server.enqueue(new MockResponse().setResponseCode(402).setBody("{\"error\":\"declined\"}"));

    assertThatThrownBy(() -> provider.initiate("request-123", BigDecimal.TEN, "MXN", "Premium"))
        .isInstanceOf(PaymentProviderException.class)
        .hasMessageContaining("HTTP 402");
  }

  private static PaymentProperties properties() {
    return new PaymentProperties(
        "stripe",
        new PaymentProperties.MercadoPagoConfig(null, null, null),
        new PaymentProperties.PayPalConfig(null, null, null),
        new PaymentProperties.ConektaConfig(null, null),
        new PaymentProperties.StripeConfig("sk_test", "whsec_test"));
  }
}
