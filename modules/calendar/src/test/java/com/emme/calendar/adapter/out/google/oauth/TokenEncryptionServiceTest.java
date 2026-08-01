package com.emme.calendar.adapter.out.google.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.calendar.adapter.out.google.provider.TokenEncryptionService;
import com.emme.calendar.configuration.GoogleOAuthConfig;
import org.junit.jupiter.api.Test;

class TokenEncryptionServiceTest {

  private final TokenEncryptionService service =
      new TokenEncryptionService(
          new GoogleOAuthConfig(
              "id", "secret", "http://localhost/callback", "12345678901234567890123456789012"));

  @Test
  void shouldRoundTripToken() {
    String original = "ya29.a0AfH6SMB...test-access-token...xyz";
    String encrypted = service.encrypt(original);
    assertThat(encrypted).isNotEqualTo(original);
    assertThat(service.decrypt(encrypted)).isEqualTo(original);
  }

  @Test
  void shouldProduceDifferentCiphertextForSamePlaintext() {
    String plaintext = "test-token-12345";
    String enc1 = service.encrypt(plaintext);
    String enc2 = service.encrypt(plaintext);
    assertThat(enc1).isNotEqualTo(enc2); // different IV each time
    assertThat(service.decrypt(enc1)).isEqualTo(plaintext);
    assertThat(service.decrypt(enc2)).isEqualTo(plaintext);
  }

  @Test
  void shouldRejectInvalidKeyLength() {
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> new TokenEncryptionService(new GoogleOAuthConfig("id", "secret", "url", "short")));
  }
}
