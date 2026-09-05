package com.emme.notification.adapter.out.provider.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

class TwilioSmsProviderContractTest {
  private MockRestServiceServer server;
  private TwilioSmsProvider provider;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = bindTo(builder).build();
    provider =
        new TwilioSmsProvider(
            builder.build(), "https://twilio.test", "AC123", "token123", "+5215550000000");
  }

  @Test
  void sendsSmsUsingTwilioBasicAuthenticationAndFormContract() {
    server
        .expect(requestTo("https://twilio.test/Accounts/AC123/Messages.json"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Basic QUMxMjM6dG9rZW4xMjM="))
        .andExpect(content().formData(formData()))
        .andRespond(withStatus(HttpStatus.CREATED).body("{\"sid\":\"SM123\"}"));

    assertThat(provider.send("+5215551111111", "Hello")).isEqualTo("twilio-SM123");
    server.verify();
  }

  @Test
  void throwsTypedProviderFailureWhenTwilioRejectsTheMessage() {
    server
        .expect(requestTo("https://twilio.test/Accounts/AC123/Messages.json"))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("rate limited"));

    assertThatThrownBy(() -> provider.send("+5215551111111", "Hello"))
        .isInstanceOf(SmsProviderException.class)
        .hasMessage("Twilio send failed: HTTP 429");
    server.verify();
  }

  private static MultiValueMap<String, String> formData() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("To", "+5215551111111");
    form.add("From", "+5215550000000");
    form.add("Body", "Hello");
    return form;
  }
}
