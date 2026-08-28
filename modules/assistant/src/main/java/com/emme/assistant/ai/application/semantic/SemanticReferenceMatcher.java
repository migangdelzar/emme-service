package com.emme.assistant.ai.application.semantic;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Framework-free cosine matcher shared by semantic classification, tools, and cache lookup. */
public final class SemanticReferenceMatcher {

  public List<SemanticMatch> rank(
      EmbeddingVector query, List<SemanticReference> references, int limit) {
    Objects.requireNonNull(query, "query must not be null");
    Objects.requireNonNull(references, "references must not be null");
    if (limit <= 0) {
      throw new IllegalArgumentException("limit must be greater than zero");
    }

    return references.stream()
        .peek(reference -> Objects.requireNonNull(reference, "reference must not be null"))
        .map(reference -> new SemanticMatch(reference.key(), cosine(query, reference.embedding())))
        .sorted(
            java.util.Comparator.comparingDouble(SemanticMatch::similarity)
                .reversed()
                .thenComparing(SemanticMatch::key))
        .limit(limit)
        .toList();
  }

  public SemanticDecision decide(
      EmbeddingVector query, List<SemanticReference> references, SemanticMatchPolicy policy) {
    Objects.requireNonNull(policy, "policy must not be null");
    return policy.decide(rank(query, references, 2));
  }

  public SemanticDecision decideAuthorized(
      EmbeddingVector query,
      List<SemanticReference> references,
      Set<String> authorizedKeys,
      SemanticMatchPolicy policy) {
    Objects.requireNonNull(authorizedKeys, "authorizedKeys must not be null");
    List<SemanticReference> authorizedReferences =
        references.stream().filter(reference -> authorizedKeys.contains(reference.key())).toList();
    return decide(query, authorizedReferences, policy);
  }

  private static double cosine(EmbeddingVector query, EmbeddingVector reference) {
    if (!query.modelVersion().equals(reference.modelVersion())) {
      throw new IllegalArgumentException("Embedding model version mismatch");
    }
    if (query.values().size() != reference.values().size()) {
      throw new IllegalArgumentException("Embedding dimensions must match");
    }

    double dot = 0.0;
    double queryNorm = 0.0;
    double referenceNorm = 0.0;
    for (int index = 0; index < query.values().size(); index++) {
      double queryValue = query.values().get(index);
      double referenceValue = reference.values().get(index);
      dot += queryValue * referenceValue;
      queryNorm += queryValue * queryValue;
      referenceNorm += referenceValue * referenceValue;
    }
    if (queryNorm == 0.0 || referenceNorm == 0.0) {
      throw new IllegalArgumentException("Embedding vector must not be zero");
    }
    return dot / (Math.sqrt(queryNorm) * Math.sqrt(referenceNorm));
  }
}
