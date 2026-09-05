package com.emme.assistant.ai.application.rag;

import com.emme.ai.contracts.rag.KnowledgeQuery;
import com.emme.ai.contracts.rag.KnowledgeRetriever;
import com.emme.ai.contracts.rag.RetrievedDocument;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.assistant.ai.application.provider.RetrievalUnavailableException;
import com.emme.assistant.ai.application.semantic.SemanticFailurePolicy;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Runs bounded retrieval attempts and answers only from an accepted context. */
public final class KnowledgeAnswerService {

  private static final String RETRIEVAL_UNAVAILABLE = "Retrieval unavailable.";
  private static final String NO_RELEVANT_DOCUMENTS = "No relevant documents were found.";

  private final KnowledgeRetriever retrieval;
  private final RetrievalQualityGate qualityGate;
  private final QueryImprover queryImprover;
  private final RagAnswerPort answer;
  private final RetrievalQualityPolicy qualityPolicy;
  private final QueryImprovementPolicy improvementPolicy;
  private final Clock clock;

  public KnowledgeAnswerService(
      KnowledgeRetriever retrieval,
      RetrievalQualityGate qualityGate,
      QueryImprover queryImprover,
      RagAnswerPort answer,
      RetrievalQualityPolicy qualityPolicy,
      QueryImprovementPolicy improvementPolicy) {
    this(
        retrieval,
        qualityGate,
        queryImprover,
        answer,
        qualityPolicy,
        improvementPolicy,
        Clock.systemUTC());
  }

  public KnowledgeAnswerService(
      KnowledgeRetriever retrieval,
      RetrievalQualityGate qualityGate,
      QueryImprover queryImprover,
      RagAnswerPort answer,
      RetrievalQualityPolicy qualityPolicy,
      QueryImprovementPolicy improvementPolicy,
      Clock clock) {
    this.retrieval = Objects.requireNonNull(retrieval, "retrieval must not be null");
    this.qualityGate = Objects.requireNonNull(qualityGate, "qualityGate must not be null");
    this.queryImprover = Objects.requireNonNull(queryImprover, "queryImprover must not be null");
    this.answer = Objects.requireNonNull(answer, "answer must not be null");
    this.qualityPolicy = Objects.requireNonNull(qualityPolicy, "qualityPolicy must not be null");
    this.improvementPolicy =
        Objects.requireNonNull(improvementPolicy, "improvementPolicy must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  public GroundedAnswer answer(
      KnowledgeQuery query, KnowledgeRoute route, AiExecutionContext context) {
    Objects.requireNonNull(query, "query must not be null");
    Objects.requireNonNull(route, "route must not be null");
    Objects.requireNonNull(context, "context must not be null");
    AiExecutionContext current = AiExecutionContextScope.requireCurrent();
    if (!current.equals(context)) {
      throw new IllegalArgumentException("context must match the current AI execution context");
    }
    Instant deadline = clock.instant().plus(improvementPolicy.maximumDuration());

    List<String> candidates = new ArrayList<>();
    candidates.add(query.text());
    RetrievalQualityDecision lastDecision = rejected("NO_DOCUMENTS");
    int attempts = 0;
    while (attempts < improvementPolicy.maximumAttempts()
        && !candidates.isEmpty()
        && clock.instant().isBefore(deadline)) {
      String candidate = candidates.remove(0);
      attempts++;
      List<RetrievedDocument> documents;
      try {
        documents =
            retrieval.search(new KnowledgeQuery(candidate, query.locale(), query.limit()), context);
      } catch (RetrievalUnavailableException unavailable) {
        return unavailable(route);
      } catch (RuntimeException failure) {
        SemanticFailurePolicy.rethrowSecurityFailure(failure);
        if (SemanticFailurePolicy.isTransientVectorOrProviderFailure(failure)) {
          return unavailable(route);
        }
        throw failure;
      }
      lastDecision = qualityGate.evaluate(route, candidate, documents, qualityPolicy);
      if (lastDecision.accepted()) {
        if (!clock.instant().isBefore(deadline)) {
          break;
        }
        String response =
            answer.answer(
                new KnowledgeQuery(candidate, query.locale(), query.limit()), documents, context);
        return new GroundedAnswer(response, route, lastDecision, true);
      }
      if (attempts >= improvementPolicy.maximumAttempts()) {
        break;
      }
      if (!clock.instant().isBefore(deadline)) {
        break;
      }
      List<String> improved =
          queryImprover.improve(query.text(), route, lastDecision, context, improvementPolicy);
      candidates.addAll(boundedDistinct(improved, query.text(), improvementPolicy));
    }

    return new GroundedAnswer(NO_RELEVANT_DOCUMENTS, route, lastDecision, false);
  }

  private static List<String> boundedDistinct(
      List<String> variants, String originalQuery, QueryImprovementPolicy policy) {
    if (variants == null || variants.isEmpty()) {
      return List.of();
    }
    if (policy.maximumVariants() == 0) {
      return List.of();
    }
    LinkedHashSet<String> bounded = new LinkedHashSet<>();
    for (String variant : variants) {
      if (variant != null
          && !variant.isBlank()
          && !variant.equals(originalQuery)
          && variant.length() <= policy.maximumQueryCharacters()) {
        bounded.add(variant);
      }
      if (bounded.size() == policy.maximumVariants()) {
        break;
      }
    }
    return List.copyOf(bounded);
  }

  private static GroundedAnswer unavailable(KnowledgeRoute route) {
    return new GroundedAnswer(
        RETRIEVAL_UNAVAILABLE, route, rejected("RETRIEVAL_UNAVAILABLE"), false);
  }

  private static RetrievalQualityDecision rejected(String reason) {
    return new RetrievalQualityDecision(false, 0.0, 0.0, 0.0, 0, 0, false, reason);
  }
}
