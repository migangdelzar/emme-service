package com.emme.assistant.ai.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.rag.KnowledgeRoute;
import com.emme.assistant.ai.application.rag.QueryImprovementPolicy;
import com.emme.assistant.ai.application.rag.QueryImprover;
import com.emme.assistant.ai.application.rag.RetrievalQualityDecision;
import com.emme.kernel.context.AiExecutionContext;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;

class SpringAiQueryImproverTest {

  private static final String ORIGINAL = "What are the hours?";
  private static final AiExecutionContext CONTEXT = context();
  private static final RetrievalQualityDecision PREVIOUS =
      new RetrievalQualityDecision(false, 0.50, 0.45, 0.05, 0, 0, false, "INSUFFICIENT_SUPPORT");

  @Test
  void appliesEnabledSpringAiStrategiesInBoundedOrder() {
    QueryTransformer compression = mock(QueryTransformer.class);
    QueryTransformer rewrite = mock(QueryTransformer.class);
    QueryTransformer translation = mock(QueryTransformer.class);
    QueryExpander expansion = mock(QueryExpander.class);
    when(compression.transform(new Query(ORIGINAL))).thenReturn(new Query("compressed"));
    when(rewrite.transform(new Query(ORIGINAL))).thenReturn(new Query("rewritten"));
    when(translation.transform(new Query(ORIGINAL))).thenReturn(new Query("translated"));
    when(expansion.expand(new Query(ORIGINAL)))
        .thenReturn(List.of(new Query("expanded-a"), new Query("expanded-b")));

    QueryImprover improver =
        new SpringAiQueryImprover(compression, rewrite, translation, expansion);

    List<String> variants =
        improver.improve(
            ORIGINAL,
            KnowledgeRoute.FAQ,
            PREVIOUS,
            CONTEXT,
            new QueryImprovementPolicy(2, 3, 200, Duration.ofSeconds(1), true, true, true, true));

    assertThat(variants).containsExactly("compressed", "rewritten", "translated");
    verify(compression).transform(new Query(ORIGINAL));
    verify(rewrite).transform(new Query(ORIGINAL));
    verify(translation).transform(new Query(ORIGINAL));
    verifyNoInteractions(expansion);
  }

  @Test
  void ignoresDisabledStrategiesAndInvalidProviderVariants() {
    QueryTransformer compression = mock(QueryTransformer.class);
    QueryTransformer rewrite = mock(QueryTransformer.class);
    QueryExpander expansion = mock(QueryExpander.class);
    Query blankProviderVariant = mock(Query.class);
    when(blankProviderVariant.text()).thenReturn("  ");
    when(rewrite.transform(new Query(ORIGINAL))).thenReturn(blankProviderVariant);
    when(expansion.expand(new Query(ORIGINAL)))
        .thenReturn(
            Arrays.asList(
                null,
                new Query(ORIGINAL),
                blankProviderVariant,
                new Query("useful"),
                new Query("useful"),
                new Query("too long")));

    QueryImprover improver = new SpringAiQueryImprover(compression, rewrite, null, expansion);

    List<String> variants =
        improver.improve(
            ORIGINAL,
            KnowledgeRoute.POLICY,
            PREVIOUS,
            CONTEXT,
            new QueryImprovementPolicy(2, 2, 7, Duration.ofSeconds(1), false, true, false, true));

    assertThat(variants).containsExactly("useful");
    verify(rewrite).transform(new Query(ORIGINAL));
    verify(expansion).expand(new Query(ORIGINAL));
    verifyNoInteractions(compression);
  }

  @Test
  void ignoresNullTransformerOutputAndStopsExpansionWhenVariantBudgetIsReached() {
    QueryTransformer compression = mock(QueryTransformer.class);
    QueryTransformer rewrite = mock(QueryTransformer.class);
    QueryTransformer translation = mock(QueryTransformer.class);
    QueryExpander expansion = mock(QueryExpander.class);
    when(compression.transform(new Query(ORIGINAL))).thenReturn(null);
    when(rewrite.transform(new Query(ORIGINAL))).thenReturn(new Query("useful"));

    QueryImprover improver =
        new SpringAiQueryImprover(compression, rewrite, translation, expansion);

    List<String> variants =
        improver.improve(
            ORIGINAL,
            KnowledgeRoute.FAQ,
            PREVIOUS,
            CONTEXT,
            new QueryImprovementPolicy(2, 1, 200, Duration.ofSeconds(1), true, true, true, true));

    assertThat(variants).containsExactly("useful");
    verify(compression).transform(new Query(ORIGINAL));
    verify(rewrite).transform(new Query(ORIGINAL));
    verifyNoInteractions(translation, expansion);
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), Set.of("ROLE_tenant_client"), id, id, "trace", "id");
  }
}
