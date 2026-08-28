package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.port.out.SemanticReferenceSearchPort;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SemanticRoutingServiceTest {

  private static final EmbeddingVector QUERY =
      new EmbeddingVector("embedding-v1", List.of(1.0f, 0.0f));

  @Test
  void classifiesIntentOnlyWhenScoreAndMarginPass() {
    RecordingReferenceSearch search =
        new RecordingReferenceSearch(
            List.of(new SemanticMatch("QUOTE_DESIGN", 0.95), new SemanticMatch("FAQ", 0.20)));
    SemanticIntentClassifier classifier =
        new SemanticIntentClassifier(search, new SemanticMatchPolicy(0.90, 0.20));

    SemanticDecision decision = classifier.classify("es-MX", QUERY);

    assertThat(decision.selectedKey()).contains("QUOTE_DESIGN");
    assertThat(search.intentLimit).isEqualTo(2);
  }

  @Test
  void abstainsWhenIntentCandidatesAreTooClose() {
    RecordingReferenceSearch search =
        new RecordingReferenceSearch(
            List.of(new SemanticMatch("QUOTE_DESIGN", 0.95), new SemanticMatch("FAQ", 0.91)));
    SemanticIntentClassifier classifier =
        new SemanticIntentClassifier(search, new SemanticMatchPolicy(0.90, 0.10));

    assertThat(classifier.classify("es-MX", QUERY).selectedKey()).isEmpty();
  }

  @Test
  void toolSelectionPassesOnlyAuthorizedKeysAndRejectsAnUnauthorizedAdapterResult() {
    RecordingReferenceSearch search =
        new RecordingReferenceSearch(List.of(new SemanticMatch("bookAppointment", 0.99)));
    SemanticToolSelector selector =
        new SemanticToolSelector(search, new SemanticMatchPolicy(0.80, 0.05));

    SemanticDecision decision = selector.select("es-MX", QUERY, Set.of("findAvailability"));

    assertThat(search.authorizedToolKeys).containsExactly("findAvailability");
    assertThat(decision.selectedKey()).isEmpty();
  }

  @Test
  void cacheReturnsOnlyAHighConfidenceCandidate() {
    RecordingCache cache =
        new RecordingCache(
            List.of(new SemanticCachePort.Candidate(UUID.randomUUID(), "cached answer", 0.97)));
    SemanticCacheResolver resolver =
        new SemanticCacheResolver(cache, new SemanticCachePolicy(0.95));

    var hit =
        resolver.lookup(new SemanticCachePort.Lookup("FAQ", "catalog-v4", "prompt-v2", QUERY));

    assertThat(hit).isPresent();
    assertThat(hit.orElseThrow().responsePayload()).isEqualTo("cached answer");
    assertThat(cache.lookup)
        .isEqualTo(new SemanticCachePort.Lookup("FAQ", "catalog-v4", "prompt-v2", QUERY));
  }

  @Test
  void cacheAbstainsBelowTheSemanticThreshold() {
    RecordingCache cache =
        new RecordingCache(
            List.of(new SemanticCachePort.Candidate(UUID.randomUUID(), "stale answer", 0.94)));
    SemanticCacheResolver resolver =
        new SemanticCacheResolver(cache, new SemanticCachePolicy(0.95));

    assertThat(
            resolver.lookup(new SemanticCachePort.Lookup("FAQ", "catalog-v4", "prompt-v2", QUERY)))
        .isEmpty();
  }

  private static final class RecordingReferenceSearch implements SemanticReferenceSearchPort {
    private final List<SemanticMatch> matches;
    private int intentLimit;
    private Set<String> authorizedToolKeys = Set.of();

    private RecordingReferenceSearch(List<SemanticMatch> matches) {
      this.matches = matches;
    }

    @Override
    public List<SemanticMatch> searchIntents(String locale, EmbeddingVector query, int limit) {
      intentLimit = limit;
      return matches;
    }

    @Override
    public List<SemanticMatch> searchTools(
        String locale, EmbeddingVector query, Set<String> authorizedKeys, int limit) {
      authorizedToolKeys = Set.copyOf(authorizedKeys);
      return matches;
    }
  }

  private static final class RecordingCache implements SemanticCachePort {
    private final List<Candidate> candidates;
    private Lookup lookup;

    private RecordingCache(List<Candidate> candidates) {
      this.candidates = candidates;
    }

    @Override
    public List<Candidate> find(Lookup lookup, int limit) {
      this.lookup = lookup;
      return candidates;
    }
  }
}
