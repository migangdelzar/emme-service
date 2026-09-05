package com.emme.payment.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.emme.payment.adapter.out.provider.conekta.ConektaProvider;
import com.emme.payment.application.port.out.PaymentProviderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ConektaProviderContractTest {

  @Test
  void initiatesAndRefundsAChargeWithConektaAuthentication() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    ConektaProvider provider = provider(builder);

    server
        .expect(requestTo("https://conekta.test/charges"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Basic cHJpdl9rZXk6"))
        .andExpect(header("Accept", "application/vnd.conekta-v2.0.0+json"))
        .andExpect(
            content()
                .json(
                    """
                    {"description":"Premium","amount":1250,"currency":"MXN","reference_id":"request-123"}
                    """))
        .andRespond(withStatus(HttpStatus.OK).body("{\"id\":\"charge-123\"}"));
    server
        .expect(requestTo("https://conekta.test/charges/charge-123/refund"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json("{\"reason\":\"duplicate\"}"))
        .andRespond(withStatus(HttpStatus.OK).body("{\"id\":\"refund-123\"}"));

    assertThat(provider.initiate("request-123", new BigDecimal("12.50"), "MXN", "Premium"))
        .extracting("providerTransactionId", "status")
        .containsExactly("charge-123", "PENDING");
    assertThat(provider.refund("charge-123", BigDecimal.TEN, "duplicate").metadata())
        .containsEntry("refund_id", "refund-123");
    server.verify();
  }

  @Test
  void preservesConektaAutoAuthorizeAndCaptureSemanticsWithoutHttpCalls() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    ConektaProvider provider = provider(builder);

    assertThat(provider.authorize("charge-123").status()).isEqualTo("AUTHORIZED");
    assertThat(provider.capture("charge-123", BigDecimal.TEN).status()).isEqualTo("CAPTURED");
    server.verify();
  }

  @Test
  void mapsConektaFailureToThePaymentProviderException() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    ConektaProvider provider = provider(builder);
    server
        .expect(requestTo("https://conekta.test/charges"))
        .andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("unavailable"));

    assertThatThrownBy(() -> provider.initiate("request-123", BigDecimal.TEN, "MXN", "Premium"))
        .isInstanceOf(PaymentProviderException.class)
        .hasMessageContaining("Conekta initiate failed: HTTP 502");
    server.verify();
  }

  private static ConektaProvider provider(RestClient.Builder builder) {
    return new ConektaProvider(
        builder.build(), new ObjectMapper(), "priv_key", "https://conekta.test");
  }
}
