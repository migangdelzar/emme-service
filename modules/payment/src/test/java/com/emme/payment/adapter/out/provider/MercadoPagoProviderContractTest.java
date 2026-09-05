package com.emme.payment.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.emme.payment.adapter.out.provider.mercadopago.MercadoPagoProvider;
import com.emme.payment.application.port.out.PaymentProviderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MercadoPagoProviderContractTest {

  @Test
  void createsACheckoutPreferenceWithBearerAndIdempotencyHeaders() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    MercadoPagoProvider provider = provider(builder);

    server
        .expect(requestTo("https://mercadopago.test/checkout/preferences"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer access-token"))
        .andExpect(header("X-Idempotency-Key", "request-123"))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "items":[{"title":"Premium","quantity":1,"unit_price":12.5,"currency_id":"MXN"}],
                      "external_reference":"request-123"
                    }
                    """))
        .andRespond(
            withStatus(HttpStatus.OK)
                .body("{\"id\":\"preference-123\",\"init_point\":\"https://pay.test/123\"}"));

    var result = provider.initiate("request-123", new BigDecimal("12.50"), "MXN", "Premium");

    assertThat(result.providerTransactionId()).isEqualTo("preference-123");
    assertThat(result.metadata()).containsEntry("init_point", "https://pay.test/123");
    server.verify();
  }

  @Test
  void preservesMercadoPagoAutoAuthorizeAndCaptureSemanticsWithoutHttpCalls() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    MercadoPagoProvider provider = provider(builder);

    assertThat(provider.authorize("payment-123").status()).isEqualTo("AUTHORIZED");
    assertThat(provider.capture("payment-123", BigDecimal.TEN).status()).isEqualTo("CAPTURED");
    server.verify();
  }

  @Test
  void rejectsInitiationWhenTheAccessTokenIsMissing() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    MercadoPagoProvider provider =
        new MercadoPagoProvider(
            builder.build(), new ObjectMapper(), null, "https://mercadopago.test");

    assertThatThrownBy(() -> provider.initiate("request-123", BigDecimal.TEN, "MXN", "Premium"))
        .isInstanceOf(PaymentProviderException.class)
        .hasMessage("app.payment.mercadopago.access-token not configured");
    server.verify();
  }

  private static MercadoPagoProvider provider(RestClient.Builder builder) {
    return new MercadoPagoProvider(
        builder.build(), new ObjectMapper(), "access-token", "https://mercadopago.test");
  }
}
