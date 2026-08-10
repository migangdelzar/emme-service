package com.emme.assistant.adapter.in.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** Verifies Meta's {@code X-Hub-Signature-256} webhook digest in constant time. */
@Component
public final class WhatsAppWebhookSignatureVerifier {
  public boolean verify(String payload, String signature, String secret) {
    if (payload == null
        || signature == null
        || !signature.startsWith("sha256=")
        || secret == null
        || secret.isBlank()) {
      return false;
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      byte[] supplied = HexFormat.of().parseHex(signature.substring("sha256=".length()));
      return MessageDigest.isEqual(computed, supplied);
    } catch (IllegalArgumentException | java.security.GeneralSecurityException exception) {
      return false;
    }
  }
}
