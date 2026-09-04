package com.emme.assistant.adapter.out.client.whatsapp;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.emme.assistant.configuration.WhatsAppProperties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class WhatsAppReplyAdapterTest {

  @Test
  void sendsAWhatsAppTextThroughTheConfiguredRestClient() {
    WhatsAppProperties properties = properties("access", "phone-123", "https://graph.test/v1");
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    RestClient restClient = builder.baseUrl(properties.apiBaseUrl()).build();

    server
        .expect(requestTo("https://graph.test/v1/phone-123/messages"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer access"))
        .andExpect(
            content()
                .json(
                    """
            {
              "messaging_product": "whatsapp",
              "recipient_type": "individual",
              "to": "recipient-1",
              "type": "text",
              "text": {"body": "hello"}
            }
            """))
        .andRespond(withStatus(HttpStatus.OK));

    new WhatsAppReplyAdapter(properties, restClient).send("recipient-1", "hello");

    server.verify();
  }

  @Test
  void doesNotCallTheProviderWhenCredentialsAreIncomplete() {
    RestClient restClient = mock(RestClient.class);
    WhatsAppProperties properties = properties("", "phone-123", "https://graph.test/v1");

    new WhatsAppReplyAdapter(properties, restClient).send("recipient-1", "hello");

    verifyNoInteractions(restClient);
  }

  @Test
  void absorbsProviderHttpFailuresSoReplyProcessingCanContinue() {
    WhatsAppProperties properties = properties("access", "phone-123", "https://graph.test/v1");
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    RestClient restClient = builder.baseUrl(properties.apiBaseUrl()).build();

    server
        .expect(requestTo("https://graph.test/v1/phone-123/messages"))
        .andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("provider unavailable"));

    assertThatCode(
            () -> new WhatsAppReplyAdapter(properties, restClient).send("recipient-1", "hello"))
        .doesNotThrowAnyException();

    server.verify();
  }

  private static WhatsAppProperties properties(
      String accessToken, String phoneNumberId, String url) {
    return new WhatsAppProperties(
        "verify", "secret", UUID.randomUUID().toString(), accessToken, phoneNumberId, url);
  }
}
