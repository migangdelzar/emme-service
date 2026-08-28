package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.domain.quote.NailDesignFeatures;
import java.util.Objects;

/** Model-facing boundary for extracting validated nail-design attributes. */
public interface NailDesignExtractor {

  ExtractionResult extract(ExtractionRequest request);

  record ExtractionRequest(String inputText, String imageStorageKey) {
    public ExtractionRequest {
      inputText = normalize(inputText);
      imageStorageKey = normalize(imageStorageKey);
      if ((inputText == null || inputText.isBlank())
          && (imageStorageKey == null || imageStorageKey.isBlank())) {
        throw new IllegalArgumentException("inputText or imageStorageKey must be provided");
      }
    }

    private static String normalize(String value) {
      return value == null || value.isBlank() ? null : value;
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
