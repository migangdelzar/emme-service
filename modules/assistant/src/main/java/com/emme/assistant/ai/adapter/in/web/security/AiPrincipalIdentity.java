package com.emme.assistant.ai.adapter.in.web.security;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Derives the internal PII-free AI principal key from a validated JWT identity. */
public final class AiPrincipalIdentity {

  private static final String NAMESPACE = "emme-ai-principal-v1:";

  private AiPrincipalIdentity() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static UUID fromTrustedClaims(String issuer, String subject) {
    if (issuer == null || issuer.isBlank()) {
      throw new IllegalArgumentException("JWT issuer must not be blank");
    }
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("JWT subject must not be blank");
    }
    return UUID.nameUUIDFromBytes(
        (NAMESPACE + issuer + "\u0000" + subject).getBytes(StandardCharsets.UTF_8));
  }
}
