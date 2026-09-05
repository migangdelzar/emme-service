package com.emme.assistant.ai.application.rag;

import com.emme.ai.contracts.rag.RetrievedDocument;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Deterministic retrieval gate that never inspects or stores document text. */
public final class DeterministicRetrievalQualityGate implements RetrievalQualityGate {

  private static final String EFFECTIVE_AT = "effectiveAt";
  private static final String LEXICAL_MATCH = "lexicalMatch";

  private final Clock clock;

  public DeterministicRetrievalQualityGate() {
    this(Clock.systemUTC());
  }

  public DeterministicRetrievalQualityGate(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public RetrievalQualityDecision evaluate(
      KnowledgeRoute route,
      String query,
      List<RetrievedDocument> documents,
      RetrievalQualityPolicy policy) {
    Objects.requireNonNull(route, "route must not be null");
    requireText(query, "query");
    Objects.requireNonNull(documents, "documents must not be null");
    Objects.requireNonNull(policy, "policy must not be null");

    List<RetrievedDocument> ranked =
        documents.stream()
            .filter(Objects::nonNull)
            .sorted(
                Comparator.comparingDouble(RetrievedDocument::score)
                    .reversed()
                    .thenComparing(RetrievedDocument::sourceId))
            .toList();
    if (ranked.isEmpty()) {
      return decision(false, 0.0, 0.0, 0.0, 0, 0, false, "NO_DOCUMENTS");
    }

    RetrievedDocument top = ranked.get(0);
    double topScore = top.score();
    double secondScore = ranked.size() > 1 ? ranked.get(1).score() : 0.0;
    double margin = ranked.size() > 1 ? topScore - secondScore : 0.0;
    int supportingDocuments = distinctSources(ranked).size();
    int freshDocuments = countFresh(ranked, policy.maximumDocumentAge());
    boolean lexicalAgreement = lexicalAgreement(ranked);

    if (topScore < policy.minimumTopScore()) {
      return decision(
          false,
          topScore,
          secondScore,
          margin,
          supportingDocuments,
          freshDocuments,
          lexicalAgreement,
          "TOP_SCORE_BELOW_THRESHOLD");
    }
    if (ranked.size() < 2 || margin < policy.minimumMargin()) {
      return decision(
          false,
          topScore,
          secondScore,
          margin,
          supportingDocuments,
          freshDocuments,
          lexicalAgreement,
          "INSUFFICIENT_MARGIN");
    }
    if (supportingDocuments < policy.minimumSupportingDocuments()) {
      return decision(
          false,
          topScore,
          secondScore,
          margin,
          supportingDocuments,
          freshDocuments,
          lexicalAgreement,
          "INSUFFICIENT_SUPPORT");
    }
    if (freshDocuments < policy.minimumSupportingDocuments()) {
      return decision(
          false,
          topScore,
          secondScore,
          margin,
          supportingDocuments,
          freshDocuments,
          lexicalAgreement,
          "STALE_DOCUMENTS");
    }
    if (policy.requireLexicalAgreement() && !lexicalAgreement) {
      return decision(
          false,
          topScore,
          secondScore,
          margin,
          supportingDocuments,
          freshDocuments,
          lexicalAgreement,
          "LEXICAL_DISAGREEMENT");
    }
    return decision(
        true,
        topScore,
        secondScore,
        margin,
        supportingDocuments,
        freshDocuments,
        lexicalAgreement,
        "ACCEPTED");
  }

  private int countFresh(List<RetrievedDocument> documents, java.time.Duration maximumAge) {
    Instant cutoff = clock.instant().minus(maximumAge);
    return (int) documents.stream().filter(document -> isFresh(document, cutoff)).count();
  }

  private static boolean isFresh(RetrievedDocument document, Instant cutoff) {
    String effectiveAt = document.metadata().get(EFFECTIVE_AT);
    if (effectiveAt == null || effectiveAt.isBlank()) {
      return true;
    }
    try {
      return !Instant.parse(effectiveAt).isBefore(cutoff);
    } catch (RuntimeException ignored) {
      return false;
    }
  }

  private static boolean lexicalAgreement(List<RetrievedDocument> ranked) {
    return ranked.stream()
        .allMatch(
            document ->
                Boolean.parseBoolean(document.metadata().getOrDefault(LEXICAL_MATCH, "false")));
  }

  private static Set<String> distinctSources(List<RetrievedDocument> documents) {
    Set<String> sourceIds = new HashSet<>();
    documents.forEach(document -> sourceIds.add(document.sourceId()));
    return sourceIds;
  }

  private static RetrievalQualityDecision decision(
      boolean accepted,
      double topScore,
      double secondScore,
      double margin,
      int supportingDocuments,
      int freshDocuments,
      boolean lexicalAgreement,
      String reasonCode) {
    return new RetrievalQualityDecision(
        accepted,
        topScore,
        secondScore,
        margin,
        supportingDocuments,
        freshDocuments,
        lexicalAgreement,
        reasonCode);
  }

  private static void requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
