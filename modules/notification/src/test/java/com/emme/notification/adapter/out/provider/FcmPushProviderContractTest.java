package com.emme.notification.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.emme.notification.adapter.out.provider.push.FcmPushProvider;
import com.emme.notification.adapter.out.provider.push.PushProviderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class FcmPushProviderContractTest {

  private static final String TOKEN_URL = "https://oauth.test/token";
  private static final String FCM_URL = "https://fcm.test/v1/projects/project-123/messages:send";

  @Test
  void exchangesJwtForATokenBeforeSendingTheFcmMessage() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    FcmPushProvider provider = provider(builder);

    server
        .expect(requestTo(TOKEN_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(
            content()
                .string(
                    containsString(
                        "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer")))
        .andRespond(withStatus(HttpStatus.OK).body("{\"access_token\":\"access-123\"}"));
    server
        .expect(requestTo(FCM_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer access-123"))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "message": {
                        "token": "device-123",
                        "notification": {"title": "Title", "body": "Body"},
                        "data": {"appointmentId": "appointment-123"}
                      }
                    }
                    """))
        .andRespond(withStatus(HttpStatus.OK).body("{\"name\":\"messages/fcm-123\"}"));

    assertThat(
            provider.send(
                "device-123", "Title", "Body", Map.of("appointmentId", "appointment-123")))
        .isEqualTo("messages/fcm-123");
    server.verify();
  }

  @Test
  void doesNotSendAnFcmMessageWhenTokenExchangeFails() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    FcmPushProvider provider = provider(builder);
    server
        .expect(requestTo(TOKEN_URL))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("invalid assertion"));

    assertThatThrownBy(() -> provider.send("device-123", "Title", "Body", Map.of()))
        .isInstanceOf(PushProviderException.class)
        .hasMessageContaining("FCM OAuth2 token request failed: HTTP 401");
    server.verify();
  }

  private static FcmPushProvider provider(RestClient.Builder builder) throws Exception {
    return new FcmPushProvider(
        builder.build(),
        new ObjectMapper(),
        TOKEN_URL,
        FCM_URL,
        "service-account@example.test",
        "project-123",
        privateKey());
  }

  private static PrivateKey privateKey() throws Exception {
    return KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate();
  }
}
