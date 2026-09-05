package com.emme.notification.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.emme.notification.adapter.out.provider.email.EmailProviderException;
import com.emme.notification.adapter.out.provider.email.SendGridProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SendGridProviderContractTest {

  @Test
  void sendsTheSendGridPayloadAndReturnsTheProviderMessageId() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    SendGridProvider provider = provider(builder);

    server
        .expect(requestTo("https://sendgrid.test/v3/mail/send"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer key-123"))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "personalizations": [{"to": [{"email": "client@example.com"}], "subject": "Subject"}],
                      "from": {"email": "noreply@emme.app", "name": "Emme"},
                      "subject": "Subject",
                      "content": [
                        {"type": "text/plain", "value": "Body"},
                        {"type": "text/html", "value": "<p>Body</p>"}
                      ]
                    }
                    """))
        .andRespond(withStatus(HttpStatus.ACCEPTED).header("X-Message-Id", "sg-123"));

    assertThat(provider.send("client@example.com", "Subject", "Body", "<p>Body</p>"))
        .isEqualTo("sg-123");
    server.verify();
  }

  @Test
  void translatesSendGridHttpFailureToTypedException() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    SendGridProvider provider = provider(builder);
    server
        .expect(requestTo("https://sendgrid.test/v3/mail/send"))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("invalid key"));

    assertThatThrownBy(() -> provider.send("client@example.com", "Subject", "Body", "<p>Body</p>"))
        .isInstanceOf(EmailProviderException.class)
        .hasMessage("SendGrid send failed: HTTP 401 — invalid key");
    server.verify();
  }

  private static SendGridProvider provider(RestClient.Builder builder) {
    return new SendGridProvider(builder.build(), "key-123", "https://sendgrid.test");
  }
}
