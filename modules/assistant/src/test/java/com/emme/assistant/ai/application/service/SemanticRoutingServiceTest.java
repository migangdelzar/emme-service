package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticReferenceSearchPort;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import com.emme.assistant.ai.application.semantic.SemanticCachePolicy;
import com.emme.assistant.ai.application.semantic.SemanticCacheResolver;
import com.emme.assistant.ai.application.semantic.SemanticDecision;
import com.emme.assistant.ai.application.semantic.SemanticIntentClassifier;
import com.emme.assistant.ai.application.semantic.SemanticMatch;
import com.emme.assistant.ai.application.semantic.SemanticMatchPolicy;
import com.emme.assistant.ai.application.semantic.SemanticToolSelector;
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
    assertThat(cache.hitId()).isEqualTo(hit.orElseThrow().id());
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

  @Test
  void cacheAbstainsWhenTheTopCandidateHasInsufficientMargin() {
    RecordingCache cache =
        new RecordingCache(
            List.of(
                new SemanticCachePort.Candidate(UUID.randomUUID(), "ambiguous answer", 0.98),
                new SemanticCachePort.Candidate(UUID.randomUUID(), "competing answer", 0.97)));
    SemanticCacheResolver resolver =
        new SemanticCacheResolver(cache, new SemanticCachePolicy(0.95, 0.05));

    assertThat(
            resolver.lookup(new SemanticCachePort.Lookup("FAQ", "catalog-v4", "prompt-v2", QUERY)))
        .isEmpty();
  }

  @Test
  void cacheAbstainsWhenNoSecondCandidateCanEstablishAConfiguredMargin() {
    RecordingCache cache =
        new RecordingCache(
            List.of(new SemanticCachePort.Candidate(UUID.randomUUID(), "only answer", 0.99)));
    SemanticCacheResolver resolver =
        new SemanticCacheResolver(cache, new SemanticCachePolicy(0.95, 0.05));

    assertThat(
            resolver.lookup(new SemanticCachePort.Lookup("FAQ", "catalog-v4", "prompt-v2", QUERY)))
        .isEmpty();
  }

  @Test
  void cacheAbstainsWhenTheDurableHitUpdateCannotConfirmTheEntryIsStillValid() {
    RecordingCache cache =
        new RecordingCache(
            List.of(new SemanticCachePort.Candidate(UUID.randomUUID(), "expired answer", 0.99)));
    cache.recordHitResult = false;
    SemanticCacheResolver resolver =
        new SemanticCacheResolver(cache, new SemanticCachePolicy(0.95));

    assertThat(
            resolver.lookup(new SemanticCachePort.Lookup("FAQ", "catalog-v4", "prompt-v2", QUERY)))
        .isEmpty();
  }

  @Test
  void recordsTopScoresAndMarginForSemanticCacheLookup() {
    RecordingCache cache =
        new RecordingCache(
            List.of(
                new SemanticCachePort.Candidate(UUID.randomUUID(), "cached answer", 0.97),
                new SemanticCachePort.Candidate(UUID.randomUUID(), "other answer", 0.80)));
    RecordingSemanticMetrics metrics = new RecordingSemanticMetrics();
    SemanticCacheResolver resolver =
        new SemanticCacheResolver(cache, new SemanticCachePolicy(0.90, 0.10), metrics);

    resolver.lookup(new SemanticCachePort.Lookup("FAQ", "catalog-v4", "prompt-v2", QUERY));

    assertThat(metrics.scoreOperation).isEqualTo("cache");
    assertThat(metrics.top1).isEqualTo(0.97);
    assertThat(metrics.top2).isEqualTo(0.80);
    assertThat(metrics.margin).isCloseTo(0.17, org.assertj.core.data.Offset.offset(0.0000001));
  }

  @Test
  void recordsBoundedRoutingOutcomesWithoutTenantCardinality() {
    RecordingReferenceSearch search =
        new RecordingReferenceSearch(List.of(new SemanticMatch("FAQ", 0.98)));
    RecordingSemanticMetrics metrics = new RecordingSemanticMetrics();
    SemanticIntentClassifier classifier =
        new SemanticIntentClassifier(search, new SemanticMatchPolicy(0.90, 0.10), metrics);

    classifier.classify("es-MX", QUERY);

    assertThat(metrics.routingOutcome).isEqualTo("accepted");
  }

  @Test
  void recordsTopScoresAndMarginForSemanticRouting() {
    RecordingReferenceSearch search =
        new RecordingReferenceSearch(
            List.of(new SemanticMatch("FAQ", 0.98), new SemanticMatch("OTHER", 0.70)));
    RecordingSemanticMetrics metrics = new RecordingSemanticMetrics();
    SemanticIntentClassifier classifier =
        new SemanticIntentClassifier(search, new SemanticMatchPolicy(0.90, 0.10), metrics);

    classifier.classify("es-MX", QUERY);

    assertThat(metrics.scoreOperation).isEqualTo("routing");
    assertThat(metrics.top1).isEqualTo(0.98);
    assertThat(metrics.top2).isEqualTo(0.70);
    assertThat(metrics.margin).isEqualTo(0.28);
  }

  @Test
  void recordsTopScoresAndMarginForSemanticToolSelection() {
    RecordingReferenceSearch search =
        new RecordingReferenceSearch(
            List.of(new SemanticMatch("findAvailability", 0.91), new SemanticMatch("book", 0.76)));
    RecordingSemanticMetrics metrics = new RecordingSemanticMetrics();
    SemanticToolSelector selector =
        new SemanticToolSelector(search, new SemanticMatchPolicy(0.80, 0.10), metrics);

    selector.select("es-MX", QUERY, Set.of("findAvailability", "book"));

    assertThat(metrics.scoreOperation).isEqualTo("tool_selection");
    assertThat(metrics.top1).isEqualTo(0.91);
    assertThat(metrics.top2).isEqualTo(0.76);
    assertThat(metrics.margin).isCloseTo(0.15, org.assertj.core.data.Offset.offset(0.0000001));
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
    private UUID hitId;
    private boolean recordHitResult = true;

    private RecordingCache(List<Candidate> candidates) {
      this.candidates = candidates;
    }

    @Override
    public List<Candidate> find(Lookup lookup, int limit) {
      this.lookup = lookup;
      return candidates;
    }

    @Override
    public UUID put(Put write) {
      return UUID.randomUUID();
    }

    @Override
    public boolean recordHit(UUID candidateId) {
      hitId = candidateId;
      return recordHitResult;
    }

    @Override
    public void invalidate(String cacheKind) {}

    private UUID hitId() {
      return hitId;
    }
  }

  private static final class RecordingSemanticMetrics implements SemanticMetrics {
    private String routingOutcome;
    private String scoreOperation;
    private double top1;
    private double top2;
    private double margin;

    @Override
    public void recordRouting(String outcome) {
      routingOutcome = outcome;
    }

    @Override
    public void recordToolSelection(String outcome) {}

    @Override
    public void recordCacheLookup(String outcome) {}

    @Override
    public void recordCacheWrite(String outcome) {}

    @Override
    public void recordScores(String operation, double top1, double top2, double margin) {
      this.scoreOperation = operation;
      this.top1 = top1;
      this.top2 = top2;
      this.margin = margin;
    }
  }
}
