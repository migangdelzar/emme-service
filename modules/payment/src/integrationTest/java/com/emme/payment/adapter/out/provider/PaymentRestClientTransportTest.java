package com.emme.payment.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.payment.adapter.out.provider.stripe.StripeProvider;
import com.emme.payment.configuration.PaymentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class PaymentRestClientTransportTest {

  @Test
  void sendsStripeIdempotencyAndAuthorizationHeadersOverRealSockets() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.start();
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody("{\"id\":\"pi-123\",\"client_secret\":\"secret\"}"));
      StripeProvider provider =
          new StripeProvider(
              new PaymentProperties(
                  "stripe",
                  new PaymentProperties.MercadoPagoConfig(null, null, null),
                  new PaymentProperties.PayPalConfig(null, null, null),
                  new PaymentProperties.ConektaConfig(null, null),
                  new PaymentProperties.StripeConfig("sk_test_transport", "whsec_test")),
              RestClient.builder().build(),
              server.url("").toString(),
              new ObjectMapper());

      var result =
          provider.initiate("idem-123", new BigDecimal("12.50"), "MXN", "Appointment hold");
      RecordedRequest request = server.takeRequest();

      assertThat(result.providerTransactionId()).isEqualTo("pi-123");
      assertThat(request.getMethod()).isEqualTo("POST");
      assertThat(request.getPath()).isEqualTo("/v1/payment_intents");
      assertThat(request.getHeader("Authorization")).isEqualTo("Bearer sk_test_transport");
      assertThat(request.getHeader("Idempotency-Key")).isEqualTo("idem-123");
      assertThat(request.getBody().readUtf8()).contains("amount=1250");
    }
  }
}
