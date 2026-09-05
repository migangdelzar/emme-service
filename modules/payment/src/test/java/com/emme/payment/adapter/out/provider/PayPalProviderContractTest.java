package com.emme.payment.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.emme.payment.adapter.out.provider.paypal.PayPalProvider;
import com.emme.payment.application.port.out.PaymentProviderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

class PayPalProviderContractTest {

  @Test
  void exchangesAnOauthTokenBeforeCreatingAnIdempotentOrder() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    PayPalProvider provider = provider(builder);
    server
        .expect(requestTo("https://paypal.test/v1/oauth2/token"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Basic Y2xpZW50LWlkOmNsaWVudC1zZWNyZXQ="))
        .andExpect(content().formData(tokenForm()))
        .andRespond(
            withStatus(HttpStatus.OK)
                .body("{\"access_token\":\"paypal-token\",\"expires_in\":3600}"));
    server
        .expect(requestTo("https://paypal.test/v2/checkout/orders"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer paypal-token"))
        .andExpect(header("PayPal-Request-Id", "request-123"))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "intent":"CAPTURE",
                      "purchase_units":[{"amount":{"currency_code":"MXN","value":"12.50"},"description":"Premium"}]
                    }
                    """))
        .andRespond(
            withStatus(HttpStatus.CREATED)
                .body(
                    "{\"id\":\"order-123\",\"links\":[{\"rel\":\"approve\",\"href\":\"https://paypal.test/approve\"}]}"));

    var result = provider.initiate("request-123", new BigDecimal("12.50"), "MXN", "Premium");

    assertThat(result.providerTransactionId()).isEqualTo("order-123");
    assertThat(result.metadata()).containsEntry("approval_url", "https://paypal.test/approve");
    server.verify();
  }

  @Test
  void stopsBeforeCreatingAnOrderWhenPaypalTokenExchangeFails() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    PayPalProvider provider = provider(builder);
    server
        .expect(requestTo("https://paypal.test/v1/oauth2/token"))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("invalid client"));

    assertThatThrownBy(() -> provider.initiate("request-123", BigDecimal.TEN, "MXN", "Premium"))
        .isInstanceOf(PaymentProviderException.class)
        .hasMessageContaining("PayPal OAuth2 failed: HTTP 401");
    server.verify();
  }

  @Test
  void preservesPaypalAutoAuthorizeAndCaptureSemanticsWithoutHttpCalls() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    PayPalProvider provider = provider(builder);

    assertThat(provider.authorize("order-123").status()).isEqualTo("AUTHORIZED");
    assertThat(provider.capture("order-123", BigDecimal.TEN).status()).isEqualTo("CAPTURED");
    server.verify();
  }

  private static PayPalProvider provider(RestClient.Builder builder) {
    return new PayPalProvider(
        builder.build(), new ObjectMapper(), "client-id", "client-secret", "https://paypal.test");
  }

  private static MultiValueMap<String, String> tokenForm() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");
    return form;
  }
}
