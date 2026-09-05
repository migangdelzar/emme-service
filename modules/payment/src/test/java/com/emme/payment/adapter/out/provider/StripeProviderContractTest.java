package com.emme.payment.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.emme.payment.adapter.out.provider.stripe.StripeProvider;
import com.emme.payment.application.port.out.PaymentProviderException;
import com.emme.payment.configuration.PaymentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

class StripeProviderContractTest {

  @Test
  void initiatesPaymentWithStripeAuthenticationAndIdempotencyContract() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    StripeProvider provider = provider(builder);
    server
        .expect(requestTo("https://stripe.test/v1/payment_intents"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer sk_test"))
        .andExpect(header("Idempotency-Key", "request-123"))
        .andExpect(
            content()
                .formData(formData("amount", "1250", "currency", "mxn", "description", "Premium")))
        .andRespond(
            withStatus(HttpStatus.OK).body("{\"id\":\"pi_123\",\"client_secret\":\"secret_123\"}"));

    var result = provider.initiate("request-123", new BigDecimal("12.50"), "MXN", "Premium");

    assertThat(result.providerTransactionId()).isEqualTo("pi_123");
    assertThat(result.status()).isEqualTo("PENDING");
    assertThat(result.metadata()).containsEntry("client_secret", "secret_123");
    server.verify();
  }

  @Test
  void mapsStripeFailureToPaymentProviderException() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    StripeProvider provider = provider(builder);
    server
        .expect(requestTo("https://stripe.test/v1/payment_intents"))
        .andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED).body("{\"error\":\"declined\"}"));

    assertThatThrownBy(() -> provider.initiate("request-123", BigDecimal.TEN, "MXN", "Premium"))
        .isInstanceOf(PaymentProviderException.class)
        .hasMessageContaining("HTTP 402");
    server.verify();
  }

  private static StripeProvider provider(RestClient.Builder builder) {
    return new StripeProvider(
        properties(), builder.build(), "https://stripe.test", new ObjectMapper());
  }

  private static MultiValueMap<String, String> formData(String... values) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    for (int index = 0; index < values.length; index += 2) {
      form.add(values[index], values[index + 1]);
    }
    return form;
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
