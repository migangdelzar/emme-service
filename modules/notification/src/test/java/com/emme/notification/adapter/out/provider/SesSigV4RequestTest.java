package com.emme.notification.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.notification.adapter.out.provider.email.SesEmailProvider;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SesSigV4RequestTest {

  @Test
  void payloadHashIsComputedFromTheUtf8BytesSentOnTheWire() {
    byte[] payload = "\u00a1Hola, Emme!".getBytes(StandardCharsets.UTF_8);

    assertThat(SesEmailProvider.sha256Hex(payload))
        .isEqualTo("4280fd1e8e8d1f5656ef017c96f932d1aa99bec7d9a12d8cc1e9fff666688659");
  }

  @Test
  void signatureContainsTheCanonicalSesSignedHeaders() {
    String authorization =
        SesEmailProvider.buildSignatureV4(
            "AKIDEXAMPLE",
            "secret-example",
            "us-east-1",
            "email.us-east-1.amazonaws.com",
            "email.us-east-1.amazonaws.com",
            "/v2/email/outbound-emails",
            "payload-hash",
            "20260905T120000Z",
            "20260905");

    assertThat(authorization)
        .startsWith("AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20260905/us-east-1/ses/aws4_request")
        .contains("SignedHeaders=content-type;host;x-amz-content-sha256;x-amz-date")
        .containsPattern("Signature=[0-9a-f]{64}");
  }
}
