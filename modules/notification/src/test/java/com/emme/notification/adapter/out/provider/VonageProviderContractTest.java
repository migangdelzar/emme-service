package com.emme.notification.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.emme.notification.adapter.out.provider.sms.SmsProviderException;
import com.emme.notification.adapter.out.provider.sms.VonageProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class VonageProviderContractTest {

  @Test
  void sendsVonageJsonContractAndReturnsProviderId() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    VonageProvider provider = provider(builder);

    server
        .expect(requestTo("https://vonage.test/sms/json"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "api_key": "key-123",
                      "api_secret": "secret-123",
                      "from": "Emme",
                      "to": "+5215551111111",
                      "text": "Hello"
                    }
                    """))
        .andRespond(
            withStatus(HttpStatus.OK).body("{\"messages\":[{\"message-id\":\"vonage-123\"}]}"));

    assertThat(provider.send("+5215551111111", "Hello")).isEqualTo("vonage-vonage-123");
    server.verify();
  }

  @Test
  void translatesProviderHttpFailureToTypedException() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    VonageProvider provider = provider(builder);
    server
        .expect(requestTo("https://vonage.test/sms/json"))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("rate limited"));

    assertThatThrownBy(() -> provider.send("+5215551111111", "Hello"))
        .isInstanceOf(SmsProviderException.class)
        .hasMessage("Vonage send failed: HTTP 429");
    server.verify();
  }

  private static VonageProvider provider(RestClient.Builder builder) {
    return new VonageProvider(
        builder.baseUrl("https://vonage.test").build(),
        "https://vonage.test",
        "key-123",
        "secret-123",
        "Emme",
        new ObjectMapper());
  }
}
