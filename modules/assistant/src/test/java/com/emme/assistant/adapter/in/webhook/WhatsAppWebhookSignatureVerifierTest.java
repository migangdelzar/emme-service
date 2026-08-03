package com.emme.assistant.adapter.in.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class WhatsAppWebhookSignatureVerifierTest {

  private final WhatsAppWebhookSignatureVerifier verifier = new WhatsAppWebhookSignatureVerifier();

  @Test
  void acceptsMetaSha256Signature() throws Exception {
    String payload = "{\"entry\":[]}";
    String secret = "app-secret";
    String signature = "sha256=" + hmac(payload, secret);

    assertThat(verifier.verify(payload, signature, secret)).isTrue();
  }

  @Test
  void rejectsMissingSecretMalformedSignatureAndWrongDigest() {
    assertThat(verifier.verify("payload", "sha256=00", "")).isFalse();
    assertThat(verifier.verify("payload", "not-a-signature", "secret")).isFalse();
    assertThat(verifier.verify("payload", "sha256=00", "secret")).isFalse();
  }

  private static String hmac(String payload, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
  }
}
