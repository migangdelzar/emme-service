package com.emme.calendar.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class GoogleOAuthPropertiesTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void acceptsA32ByteEncryptionKey() {
    GoogleOAuthProperties properties =
        new GoogleOAuthProperties(
            "client-id",
            "client-secret",
            "http://localhost/callback",
            "12345678901234567890123456789012");

    assertThat(validator.validate(properties)).isEmpty();
  }

  @Test
  void rejectsAnEncryptionKeyThatIsNotExactly32Bytes() {
    GoogleOAuthProperties properties =
        new GoogleOAuthProperties(
            "client-id", "client-secret", "http://localhost/callback", "short");

    assertThat(validator.validate(properties))
        .anyMatch(violation -> violation.getPropertyPath().toString().equals("encryptionKey"));
  }
}
