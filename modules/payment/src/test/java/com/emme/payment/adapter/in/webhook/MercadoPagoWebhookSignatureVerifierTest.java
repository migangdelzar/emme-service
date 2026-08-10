package com.emme.payment.adapter.in.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class MercadoPagoWebhookSignatureVerifierTest {

  private final MercadoPagoWebhookSignatureVerifier verifier =
      new MercadoPagoWebhookSignatureVerifier();

  @Test
  void acceptsMercadoPagoManifestSignature() throws Exception {
    String secret = "webhook-secret";
    String manifest = "id:payment-1;request-id:request-1;ts:1704908010;";
    String signature = "ts=1704908010,v1=" + hmacHex(manifest, secret);

    assertThat(verifier.verify(signature, "request-1", "payment-1", secret)).isTrue();
  }

  @Test
  void rejectsMissingSecretMalformedSignatureAndWrongRequestMetadata() throws Exception {
    String signature =
        "ts=1704908010,v1=" + hmacHex("id:payment-1;request-id:request-1;ts:1704908010;", "secret");

    assertThat(verifier.verify(signature, "request-1", "payment-1", "")).isFalse();
    assertThat(verifier.verify("not-a-signature", "request-1", "payment-1", "secret")).isFalse();
    assertThat(verifier.verify(signature, "different-request", "payment-1", "secret")).isFalse();
  }

  private static String hmacHex(String message, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    StringBuilder result = new StringBuilder();
    for (byte value : mac.doFinal(message.getBytes(StandardCharsets.UTF_8))) {
      result.append(String.format("%02x", value));
    }
    return result.toString();
  }
}
