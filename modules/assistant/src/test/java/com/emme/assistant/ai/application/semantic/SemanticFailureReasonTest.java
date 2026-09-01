package com.emme.assistant.ai.application.semantic;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.security.access.AccessDeniedException;

class SemanticFailureReasonTest {

  @Test
  void mapsKnownFailureCategoriesToStableBoundedCodes() {
    assertThat(SemanticFailureReason.code(new EmbeddingProviderUnavailableException("offline")))
        .isEqualTo("embedding_unavailable");
    assertThat(SemanticFailureReason.code(new TransientDataAccessResourceException("offline")))
        .isEqualTo("transient_data_store");
    assertThat(SemanticFailureReason.code(new AccessDeniedException("forbidden")))
        .isEqualTo("security_failure");
    assertThat(SemanticFailureReason.code(new IllegalArgumentException("invalid")))
        .isEqualTo("invalid_input");
  }

  @Test
  void mapsWrappedAndUnknownExceptionsWithoutUsingTheirClassNames() {
    String reason =
        SemanticFailureReason.code(
            new RuntimeException(
                "outer", new EmbeddingProviderUnavailableException("provider offline")));
    String unknownReason = SemanticFailureReason.code(new ExceptionWithAnUnboundedName());

    assertThat(reason).isEqualTo("embedding_unavailable");
    assertThat(unknownReason).isEqualTo("unexpected_failure");
    assertThat(unknownReason).hasSizeLessThan(32);
  }

  private static final class ExceptionWithAnUnboundedName extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }
}
