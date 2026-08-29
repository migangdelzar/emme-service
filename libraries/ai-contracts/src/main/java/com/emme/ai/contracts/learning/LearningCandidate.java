package com.emme.ai.contracts.learning;

import java.util.Objects;
import java.util.regex.Pattern;

/** A redacted, versioned learning example awaiting evaluation and promotion. */
public record LearningCandidate(
    String candidateKey,
    LearningCandidateKind kind,
    String referenceText,
    String locale,
    String embeddingModelVersion,
    LearningCandidateEvidence evidence) {

  private static final Pattern EMAIL =
      Pattern.compile(
          "(?i)(?<![A-Za-z0-9._%+-])[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(?![A-Za-z0-9.-])");
  private static final Pattern PHONE =
      Pattern.compile("(?<!\\d)(?:\\+?\\d[\\d ()-]{7,}\\d)(?!\\d)");
  private static final Pattern BEARER = Pattern.compile("(?i)\\bBearer\\s+\\S+");

  public LearningCandidate {
    requireText(candidateKey, "candidateKey", 160);
    Objects.requireNonNull(kind, "kind must not be null");
    requireText(referenceText, "referenceText", 4000);
    if (EMAIL.matcher(referenceText).find()
        || PHONE.matcher(referenceText).find()
        || BEARER.matcher(referenceText).find()) {
      throw new IllegalArgumentException("referenceText must be PII-redacted");
    }
    requireText(locale, "locale", 32);
    requireText(embeddingModelVersion, "embeddingModelVersion", 150);
    Objects.requireNonNull(evidence, "evidence must not be null");
  }

  private static void requireText(String value, String field, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    if (value.length() > maxLength) {
      throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters");
    }
  }
}
