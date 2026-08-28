package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.domain.quote.NailDesignFeatures;
import java.util.Objects;

/** Model-facing boundary for extracting validated nail-design attributes. */
public interface NailDesignExtractor {

  ExtractionResult extract(ExtractionRequest request);

  record ExtractionRequest(String inputText, String imageStorageKey) {
    public ExtractionRequest {
      if ((inputText == null || inputText.isBlank())
          && (imageStorageKey == null || imageStorageKey.isBlank())) {
        throw new IllegalArgumentException("inputText or imageStorageKey must be provided");
      }
    }
  }

  record ExtractionResult(
      NailDesignFeatures features,
      String modelVersion,
      String promptVersion,
      String schemaVersion) {
    public ExtractionResult {
      Objects.requireNonNull(features, "features must not be null");
      requireText(modelVersion, "modelVersion");
      requireText(promptVersion, "promptVersion");
      requireText(schemaVersion, "schemaVersion");
    }

    private static void requireText(String value, String field) {
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException(field + " must not be blank");
      }
    }
  }
}
