package com.emme.ai.contracts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.emme.ai.contracts.extraction.ArtComplexity;
import com.emme.ai.contracts.extraction.NailDesignFeatures;
import com.emme.ai.contracts.model.ModelCapability;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.ai.contracts.routing.IntentDefinition;
import com.emme.ai.contracts.routing.IntentMatch;
import com.emme.ai.contracts.routing.IntentRoute;
import com.emme.ai.contracts.routing.RouteRequest;
import com.emme.ai.contracts.semantic.EmbeddingModelVersion;
import com.emme.ai.contracts.semantic.EmbeddingVector;
import com.emme.ai.contracts.semantic.SemanticCacheEntry;
import com.emme.ai.contracts.tool.ToolRisk;
import com.emme.ai.contracts.workflow.WorkflowStatus;
import com.emme.kernel.context.AiExecutionContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

class PlatformContractTest {

  @Test
  void executionContextCopiesRolesAndRejectsMissingTrustedIdentity() {
    UUID tenantId = UUID.randomUUID();
    UUID principalId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID workflowId = UUID.randomUUID();
    var mutableRoles = new java.util.HashSet<>(Set.of("SALON_OWNER"));

    var context =
        new AiExecutionContext(
            tenantId,
            principalId,
            mutableRoles,
            conversationId,
            workflowId,
            "trace-1",
            "idempotency-1");

    mutableRoles.add("ADMIN");

    assertThat(context.roles()).containsExactly("SALON_OWNER");
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new AiExecutionContext(
                    null,
                    principalId,
                    Set.of(),
                    conversationId,
                    workflowId,
                    "trace-1",
                    "idempotency-1"));
  }

  @Test
  void modelAdmissionUsesTheCanonicalKernelExecutionContext() {
    var context = kernelExecutionContext();
    ModelExecutionScheduler scheduler =
        new ModelExecutionScheduler() {
          @Override
          public <T> T execute(
              ModelCapability capability,
              AiExecutionContext executionContext,
              java.time.Duration timeout,
              Callable<T> operation) {
            assertThat(executionContext).isSameAs(context);
            try {
              return operation.call();
            } catch (Exception exception) {
              throw new RuntimeException(exception);
            }
          }
        };

    assertThat(
            scheduler.execute(
                ModelCapability.EMBEDDING,
                context,
                java.time.Duration.ofSeconds(1),
                () -> "admitted"))
        .isEqualTo("admitted");
  }

  @Test
  void embeddingVectorRequiresFiniteValuesAndMatchingDimension() {
    var model =
        new EmbeddingModelVersion(
            "embeddinggemma",
            "v1",
            3,
            com.emme.ai.contracts.semantic.DistanceMetric.COSINE,
            "query-v1");

    var vector = new EmbeddingVector(List.of(0.1f, 0.2f, 0.3f), model);

    assertThat(vector.values()).containsExactly(0.1f, 0.2f, 0.3f);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new EmbeddingVector(List.of(0.1f, Float.NaN), model));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new EmbeddingVector(List.of(0.1f, 0.2f), model));
  }

  @Test
  void nailDesignFeaturesRejectsInvalidConfidenceAndCopiesCollections() {
    var effects = new java.util.ArrayList<com.emme.ai.contracts.extraction.NailEffect>();
    var ambiguities = new java.util.ArrayList<>(List.of("length unclear"));
    var features =
        new NailDesignFeatures(
            null,
            null,
            "nude",
            effects,
            List.of(),
            null,
            null,
            null,
            ArtComplexity.MODERATE,
            Map.of("artComplexity", 0.8),
            ambiguities,
            true);

    effects.add(com.emme.ai.contracts.extraction.NailEffect.CHROME);
    ambiguities.clear();

    assertThat(features.effects()).isEmpty();
    assertThat(features.ambiguities()).containsExactly("length unclear");
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new NailDesignFeatures(
                    null,
                    null,
                    null,
                    List.of(),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    Map.of("shape", 1.1),
                    List.of(),
                    false));
  }

  @Test
  void routeIncludesTopTwoEvidenceAndAbstainsWhenConfidenceGateFails() {
    var request = new RouteRequest("cuánto cuesta el diseño", "es-MX", Set.of("QUOTE_DESIGN"));
    var definition =
        new IntentDefinition(
            "QUOTE_DESIGN",
            "Calculate a design quote",
            List.of("cuánto cuesta este diseño"),
            "calculateQuote",
            Set.of("design"),
            Set.of("CLIENT", "STAFF"),
            ToolRisk.READ_ONLY,
            true,
            true);
    var top = new IntentMatch(definition, 0.91);
    var second =
        new IntentMatch(
            new IntentDefinition(
                "SERVICE_INFORMATION",
                "Explain a service",
                List.of("qué servicios tienen"),
                "getSalonServices",
                Set.of(),
                Set.of("CLIENT"),
                ToolRisk.READ_ONLY,
                false,
                false),
            0.72);

    var route = IntentRoute.abstained(request, top, second, 0.19, false, "missing design slot");

    assertThat(route.top1()).isEqualTo(top);
    assertThat(route.top2()).isEqualTo(second);
    assertThat(route.margin()).isEqualTo(0.19);
    assertThat(route.abstained()).isTrue();
    assertThat(route.abstainReason()).contains("missing design slot");
  }

  @Test
  void semanticCacheEntryRequiresFutureExpiryAndCopiesResponseMetadata() {
    var entry =
        new SemanticCacheEntry(
            "cache-1",
            UUID.randomUUID(),
            "client",
            new EmbeddingVector(
                List.of(0.1f, 0.2f, 0.3f),
                new EmbeddingModelVersion(
                    "embeddinggemma",
                    "v1",
                    3,
                    com.emme.ai.contracts.semantic.DistanceMetric.COSINE,
                    "query-v1")),
            "¿Cuál es el horario?",
            "Abrimos de 9:00 a 18:00.",
            "prompt-v1",
            "policy-v1",
            Instant.now().plusSeconds(60));

    assertThat(entry.response()).isEqualTo("Abrimos de 9:00 a 18:00.");
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new SemanticCacheEntry(
                    "cache-1",
                    UUID.randomUUID(),
                    "client",
                    entry.embedding(),
                    "question",
                    "answer",
                    "prompt-v1",
                    "policy-v1",
                    Instant.now().minusSeconds(1)));
  }

  @Test
  void workflowStatusesExposeOnlyExplicitTerminalStates() {
    assertThat(WorkflowStatus.SUCCEEDED.isTerminal()).isTrue();
    assertThat(WorkflowStatus.WAITING_FOR_APPROVAL.isTerminal()).isFalse();
    assertThat(WorkflowStatus.WAITING_FOR_CONFIRMATION.isTerminal()).isFalse();
    assertThat(WorkflowStatus.WAITING_FOR_PAYMENT.isTerminal()).isFalse();
    assertThat(WorkflowStatus.RUNNING.isTerminal()).isFalse();
    assertThat(WorkflowStatus.FAILED.isTerminal()).isTrue();
  }

  private static AiExecutionContext kernelExecutionContext() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-kernel",
        "idempotency-kernel");
  }
}
