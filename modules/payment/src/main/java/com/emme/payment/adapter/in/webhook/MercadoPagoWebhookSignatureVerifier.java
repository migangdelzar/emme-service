package com.emme.payment.adapter.in.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Verifies MercadoPago HMAC-SHA256 webhook signatures without leaking timing information. */
public final class MercadoPagoWebhookSignatureVerifier {
  public boolean verify(String signature, String requestId, String dataId, String secret) {
    if (signature == null
        || signature.isBlank()
        || requestId == null
        || requestId.isBlank()
        || dataId == null
        || dataId.isBlank()
        || secret == null
        || secret.isBlank()) {
      return false;
    }
    try {
      String timestamp = value(signature, "ts");
      String supplied = value(signature, "v1");
      if (timestamp == null || supplied == null) {
        return false;
      }
      String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + timestamp + ";";
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] expected = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
      byte[] received = hexToBytes(supplied);
      return MessageDigest.isEqual(expected, received);
    } catch (IllegalArgumentException | java.security.GeneralSecurityException exception) {
      return false;
    }
  }

  private String value(String signature, String name) {
    for (String part : signature.split(",")) {
      String[] pair = part.trim().split("=", 2);
      if (pair.length == 2 && pair[0].equals(name)) {
        return pair[1];
      }
    }
    return null;
  }

  private byte[] hexToBytes(String value) {
    if (value.length() % 2 != 0) {
      throw new IllegalArgumentException("Invalid hexadecimal signature");
    }
    byte[] result = new byte[value.length() / 2];
    for (int index = 0; index < value.length(); index += 2) {
      result[index / 2] = (byte) Integer.parseInt(value.substring(index, index + 2), 16);
    }
    return result;
  }
}
