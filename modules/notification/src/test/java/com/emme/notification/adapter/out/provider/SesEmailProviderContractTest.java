package com.emme.notification.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.emme.notification.adapter.out.provider.email.EmailProviderException;
import com.emme.notification.adapter.out.provider.email.SesEmailProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SesEmailProviderContractTest {

  @Test
  void signsAndSendsTheExactUtf8SesPayloadBytes() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    SesEmailProvider provider = provider(builder);
    String expectedBody = expectedBody();
    String expectedBodyHash =
        SesEmailProvider.sha256Hex(expectedBody.getBytes(StandardCharsets.UTF_8));

    server
        .expect(requestTo("https://ses.test/v2/email/outbound-emails"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            org.springframework.test.web.client.match.MockRestRequestMatchers.content()
                .string(expectedBody))
        .andExpect(
            request -> {
              String amzDate = request.getHeaders().getFirst("X-Amz-Date");
              String dateStamp = amzDate.substring(0, 8);
              String expectedAuthorization =
                  SesEmailProvider.buildSignatureV4(
                      "AKIDEXAMPLE",
                      "secret-example",
                      "us-east-1",
                      "email.us-east-1.amazonaws.com",
                      "ses.test",
                      "/v2/email/outbound-emails",
                      expectedBodyHash,
                      amzDate,
                      dateStamp);
              assertThat(request.getHeaders().getFirst("Content-Type"))
                  .startsWith("application/json");
              assertThat(request.getHeaders().getFirst("X-Amz-Content-Sha256"))
                  .isEqualTo(expectedBodyHash);
              assertThat(request.getHeaders().getFirst("Authorization"))
                  .isEqualTo(expectedAuthorization);
            })
        .andRespond(withStatus(HttpStatus.OK).header("X-Amzn-Message-Id", "ses-123"));

    assertThat(provider.send("client@example.com", "Subject", "Body", "<p>Body</p>"))
        .isEqualTo("ses-123");
    server.verify();
  }

  @Test
  void translatesSesProviderFailureToTypedException() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = bindTo(builder).build();
    SesEmailProvider provider = provider(builder);
    server
        .expect(requestTo("https://ses.test/v2/email/outbound-emails"))
        .andRespond(withStatus(HttpStatus.FORBIDDEN).body("signature mismatch"));

    assertThatThrownBy(() -> provider.send("client@example.com", "Subject", "Body", null))
        .isInstanceOf(EmailProviderException.class)
        .hasMessageContaining("AWS SES send failed: HTTP 403");
    server.verify();
  }

  private static SesEmailProvider provider(RestClient.Builder builder) {
    return new SesEmailProvider(
        builder.build(),
        "AKIDEXAMPLE",
        "secret-example",
        "us-east-1",
        "https://ses.test",
        new ObjectMapper());
  }

  private static String expectedBody() {
    Map<String, Object> emailContent =
        new java.util.TreeMap<>(
            Map.of(
                "Html", Map.of("Charset", "UTF-8", "Data", "<p>Body</p>"),
                "Text", Map.of("Charset", "UTF-8", "Data", "Body")));
    Map<String, Object> message =
        new java.util.TreeMap<>(
            Map.of("Subject", Map.of("Charset", "UTF-8", "Data", "Subject"), "Body", emailContent));
    return writeJson(
        Map.of(
            "FromEmailAddress", "noreply@emme.app",
            "Destination", Map.of("ToAddresses", List.of("client@example.com")),
            "Content", Map.of("Simple", message)));
  }

  private static String writeJson(Map<String, Object> value) {
    try {
      return new ObjectMapper().writeValueAsString(value);
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }
}
