package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SemanticReferenceMatcherTest {

  private final SemanticReferenceMatcher matcher = new SemanticReferenceMatcher();

  @Test
  void ranksReferencesByCosineSimilarityWithDeterministicTieBreaking() {
    EmbeddingVector query = vector("local-v1", 1f, 0f);
    List<SemanticReference> references =
        List.of(
            reference("BOOK_APPOINTMENT", 1f, 0f),
            reference("CHECK_AVAILABILITY", 0.8f, 0.6f),
            reference("QUOTE_DESIGN", 0f, 1f));

    List<SemanticMatch> matches = matcher.rank(query, references, 3);

    assertThat(matches)
        .extracting(SemanticMatch::key)
        .containsExactly("BOOK_APPOINTMENT", "CHECK_AVAILABILITY", "QUOTE_DESIGN");
    assertThat(matches.get(0).similarity()).isEqualTo(1.0);
    assertThat(matches.get(1).similarity()).isCloseTo(0.8, offset(0.000001));
  }

  @Test
  void selectsAReferenceOnlyWhenTopScoreAndMarginMeetTheConfiguredPolicy() {
    SemanticMatchPolicy policy = new SemanticMatchPolicy(0.75, 0.10);
    List<SemanticReference> references =
        List.of(reference("QUOTE_DESIGN", 0.98f, 0.2f), reference("FAQ", 0.8f, 0.6f));

    SemanticDecision decision = matcher.decide(vector("local-v1", 1f, 0f), references, policy);

    assertThat(decision.accepted()).isTrue();
    assertThat(decision.selectedKey()).contains("QUOTE_DESIGN");
    assertThat(decision.top1Similarity()).isGreaterThan(0.75);
    assertThat(decision.margin()).isGreaterThanOrEqualTo(0.10);
  }

  @Test
  void abstainsWhenTheTopScoreIsBelowTheThreshold() {
    SemanticMatchPolicy policy = new SemanticMatchPolicy(0.95, 0.05);

    SemanticDecision decision =
        matcher.decide(
            vector("local-v1", 1f, 0f), List.of(reference("SALON_POLICY", 0.8f, 0.6f)), policy);

    assertThat(decision.accepted()).isFalse();
    assertThat(decision.selectedKey()).isEmpty();
  }

  @Test
  void abstainsCleanlyForAStandaloneNegativeSimilarity() {
    SemanticDecision decision =
        matcher.decide(
            vector("local-v1", 1f, 0f),
            List.of(reference("UNRELATED", -1f, 0f)),
            new SemanticMatchPolicy(-1.0, 0.10));

    assertThat(decision.accepted()).isFalse();
    assertThat(decision.top2Similarity()).isEqualTo(-1.0);
    assertThat(decision.margin()).isEqualTo(0.0);
  }

  @Test
  void abstainsWhenTheTopTwoCandidatesAreTooClose() {
    SemanticMatchPolicy policy = new SemanticMatchPolicy(0.70, 0.20);

    SemanticDecision decision =
        matcher.decide(
            vector("local-v1", 1f, 0f),
            List.of(
                reference("CHECK_AVAILABILITY", 0.95f, 0.3122499f),
                reference("BOOK_APPOINTMENT", 0.94f, 0.3411747f)),
            policy);

    assertThat(decision.accepted()).isFalse();
    assertThat(decision.margin()).isLessThan(0.20);
  }

  @Test
  void restrictsSemanticSelectionToBackendAuthorizedCandidates() {
    SemanticMatchPolicy policy = new SemanticMatchPolicy(0.70, 0.05);

    SemanticDecision decision =
        matcher.decideAuthorized(
            vector("local-v1", 1f, 0f),
            List.of(reference("UNAUTHORIZED_TOOL", 1f, 0f), reference("SAFE_TOOL", 0.9f, 0.1f)),
            Set.of("SAFE_TOOL"),
            policy);

    assertThat(decision.accepted()).isTrue();
    assertThat(decision.selectedKey()).contains("SAFE_TOOL");
  }

  @Test
  void rejectsMismatchedEmbeddingModelsAndDimensions() {
    assertThatThrownBy(
            () ->
                matcher.rank(
                    vector("local-v1", 1f, 0f),
                    List.of(new SemanticReference("FAQ", vector("cloud-v2", 1f, 0f))),
                    1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Embedding model version mismatch");

    assertThatThrownBy(
            () ->
                matcher.rank(vector("local-v1", 1f, 0f), List.of(reference("FAQ", 1f, 0f, 0f)), 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Embedding dimensions must match");
  }

  @Test
  void rejectsInvalidLimitsAndZeroVectors() {
    assertThatThrownBy(() -> matcher.rank(vector("local-v1", 1f, 0f), List.of(), 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("limit must be greater than zero");
    assertThatThrownBy(
            () -> matcher.rank(vector("local-v1", 0f, 0f), List.of(reference("FAQ", 1f, 0f)), 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Embedding vector must not be zero");
  }

  private static SemanticReference reference(String key, float... values) {
    return new SemanticReference(key, vector("local-v1", values));
  }

  private static EmbeddingVector vector(String modelVersion, float... values) {
    List<Float> components = new ArrayList<>(values.length);
    for (float value : values) {
      components.add(value);
    }
    return new EmbeddingVector(modelVersion, components);
  }
}
