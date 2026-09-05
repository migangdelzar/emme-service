package com.emme.notification.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.emme.notification.adapter.out.provider.sms.MessageBirdProvider;
import com.emme.notification.adapter.out.provider.sms.SmsProviderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MessageBirdProviderContractTest {

  @Test
  void sendsMessageBirdJsonContractAndReturnsProviderId() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    MessageBirdProvider provider = provider(builder);

    server
        .expect(requestTo("https://messagebird.test/messages"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "AccessKey key-123"))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "recipients": ["+5215551111111"],
                      "originator": "Emme",
                      "body": "Hello"
                    }
                    """))
        .andRespond(withStatus(HttpStatus.CREATED).body("{\"id\":\"mb-123\"}"));

    assertThat(provider.send("+5215551111111", "Hello")).isEqualTo("messagebird-mb-123");
    server.verify();
  }

  @Test
  void translatesProviderHttpFailureToTypedException() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    MessageBirdProvider provider = provider(builder);
    server
        .expect(requestTo("https://messagebird.test/messages"))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("unavailable"));

    assertThatThrownBy(() -> provider.send("+5215551111111", "Hello"))
        .isInstanceOf(SmsProviderException.class)
        .hasMessage("MessageBird send failed: HTTP 503");
    server.verify();
  }

  private static MessageBirdProvider provider(RestClient.Builder builder) {
    return new MessageBirdProvider(
        builder.baseUrl("https://messagebird.test").build(),
        "https://messagebird.test",
        "key-123",
        "Emme",
        new ObjectMapper());
  }
}
