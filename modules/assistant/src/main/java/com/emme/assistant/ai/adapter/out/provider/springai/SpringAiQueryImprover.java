package com.emme.assistant.ai.adapter.out.provider.springai;

import com.emme.assistant.ai.application.rag.KnowledgeRoute;
import com.emme.assistant.ai.application.rag.QueryImprovementPolicy;
import com.emme.assistant.ai.application.rag.QueryImprover;
import com.emme.assistant.ai.application.rag.RetrievalQualityDecision;
import com.emme.kernel.context.AiExecutionContext;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;

/** Adapts Spring AI query transformers and expanders to the application query-improvement port. */
public final class SpringAiQueryImprover implements QueryImprover {

  private final QueryTransformer compression;
  private final QueryTransformer rewrite;
  private final QueryTransformer translation;
  private final QueryExpander expansion;

  public SpringAiQueryImprover(
      QueryTransformer compression,
      QueryTransformer rewrite,
      QueryTransformer translation,
      QueryExpander expansion) {
    this.compression = compression;
    this.rewrite = rewrite;
    this.translation = translation;
    this.expansion = expansion;
  }

  @Override
  public List<String> improve(
      String originalQuery,
      KnowledgeRoute route,
      RetrievalQualityDecision previous,
      AiExecutionContext context,
      QueryImprovementPolicy policy) {
    Objects.requireNonNull(originalQuery, "originalQuery must not be null");
    if (originalQuery.isBlank()) {
      throw new IllegalArgumentException("originalQuery must not be blank");
    }
    Objects.requireNonNull(route, "route must not be null");
    Objects.requireNonNull(previous, "previous must not be null");
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(policy, "policy must not be null");

    if (policy.maximumVariants() == 0) {
      return List.of();
    }

    Set<String> variants = new LinkedHashSet<>();
    Query query = new Query(originalQuery);
    addIfEnabled(variants, policy.allowCompression(), compression, query, originalQuery, policy);
    addIfEnabled(variants, policy.allowRewrite(), rewrite, query, originalQuery, policy);
    addIfEnabled(variants, policy.allowTranslation(), translation, query, originalQuery, policy);
    if (variants.size() < policy.maximumVariants()
        && policy.allowExpansion()
        && expansion != null) {
      List<Query> expandedQueries = expansion.expand(query);
      if (expandedQueries != null) {
        for (Query expandedQuery : expandedQueries) {
          if (variants.size() >= policy.maximumVariants()) {
            break;
          }
          addCandidate(
              variants, expandedQuery == null ? null : expandedQuery.text(), originalQuery, policy);
        }
      }
    }
    return variants.stream().limit(policy.maximumVariants()).toList();
  }

  private static void addIfEnabled(
      Set<String> variants,
      boolean enabled,
      QueryTransformer transformer,
      Query query,
      String originalQuery,
      QueryImprovementPolicy policy) {
    if (enabled && transformer != null && variants.size() < policy.maximumVariants()) {
      Query transformedQuery = transformer.transform(query);
      addCandidate(
          variants,
          transformedQuery == null ? null : transformedQuery.text(),
          originalQuery,
          policy);
    }
  }

  private static void addCandidate(
      Set<String> variants, String candidate, String originalQuery, QueryImprovementPolicy policy) {
    if (candidate == null || candidate.isBlank() || candidate.equals(originalQuery)) {
      return;
    }
    if (policy != null && candidate.length() > policy.maximumQueryCharacters()) {
      return;
    }
    variants.add(candidate);
  }
}
