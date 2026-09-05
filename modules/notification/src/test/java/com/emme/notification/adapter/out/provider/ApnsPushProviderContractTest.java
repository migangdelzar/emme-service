package com.emme.notification.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.emme.notification.adapter.out.provider.push.ApnsPushProvider;
import com.emme.notification.adapter.out.provider.push.PushProviderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ApnsPushProviderContractTest {

  @Test
  void sendsJwtAuthenticatedApnsAlertWithRequiredHeaders() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    ApnsPushProvider provider = provider(builder);

    server
        .expect(requestTo("https://apns.test/3/device/device-123"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("apns-topic", "com.emme.app"))
        .andExpect(header("apns-push-type", "alert"))
        .andExpect(header("authorization", org.hamcrest.Matchers.startsWith("bearer ey")))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "aps": {"alert": {"title": "Title", "body": "Body"}},
                      "appointmentId": "appointment-123"
                    }
                    """))
        .andRespond(withStatus(HttpStatus.OK).header("apns-id", "apns-123"));

    assertThat(
            provider.send(
                "device-123", "Title", "Body", Map.of("appointmentId", "appointment-123")))
        .isEqualTo("apns-123");
    server.verify();
  }

  @Test
  void translatesApnsProviderFailureToTypedException() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    ApnsPushProvider provider = provider(builder);
    server
        .expect(requestTo("https://apns.test/3/device/device-123"))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("invalid token"));

    assertThatThrownBy(() -> provider.send("device-123", "Title", "Body", Map.of()))
        .isInstanceOf(PushProviderException.class)
        .hasMessageContaining("APNs send failed: HTTP 400");
    server.verify();
  }

  private static ApnsPushProvider provider(RestClient.Builder builder) throws Exception {
    return new ApnsPushProvider(
        builder.build(),
        new ObjectMapper(),
        "https://apns.test",
        "key-123",
        "team-123",
        "com.emme.app",
        privateKey());
  }

  private static PrivateKey privateKey() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
    generator.initialize(256);
    return generator.generateKeyPair().getPrivate();
  }
}
