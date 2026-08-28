package com.emme.assistant.ai.application.trace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiTraceRedactorTest {

  @Test
  void redactsCommonPersonalDataAndBearerSecretsBeforePersistence() {
    AiTraceRedactor redactor = new AiTraceRedactor();

    String redacted =
        redactor.redact(
            "Contact ana@example.com or +52 55 1234 5678 with Bearer super-secret-token");

    assertThat(redacted)
        .isEqualTo("Contact [REDACTED_EMAIL] or [REDACTED_PHONE] with [REDACTED_BEARER]");
  }

  @Test
  void leavesNullPayloadsNull() {
    assertThat(new AiTraceRedactor().redact(null)).isNull();
  }
}
