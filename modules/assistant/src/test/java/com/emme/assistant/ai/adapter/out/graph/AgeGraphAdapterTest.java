package com.emme.assistant.ai.adapter.out.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.ai.contracts.graph.GraphNode;
import com.emme.ai.contracts.graph.GraphNodeType;
import com.emme.ai.contracts.graph.GraphProjection;
import com.emme.ai.contracts.graph.GraphTraversalKind;
import com.emme.ai.contracts.graph.GraphTraversalQuery;
import com.emme.assistant.ai.configuration.SpringAiAgeProperties;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AgeGraphAdapterTest {

  @Test
  void derivesTheTenantGraphNameFromTheBoundBackendContext() {
    RecordingAgeGraphClient client = new RecordingAgeGraphClient(true);
    AgeGraphAdapter adapter =
        new AgeGraphAdapter(client, new SpringAiAgeProperties(true, "emme_ai_graph_", 5));
    AiExecutionContext context = context();
    GraphProjection projection =
        new GraphProjection(
            List.of(new GraphNode(GraphNodeType.DESIGN, UUID.randomUUID(), Map.of())), List.of());

    AiExecutionContextScope.run(context, () -> adapter.project(projection, context));

    assertThat(client.projectedTenant()).isEqualTo(context.tenantId());
    assertThat(client.projectedGraphName())
        .isEqualTo("emme_ai_graph_" + context.tenantId().toString().replace("-", ""));
  }

  @Test
  void returnsNoRecommendationsWhenAgeIsUnavailable() {
    AgeGraphAdapter adapter =
        new AgeGraphAdapter(
            new RecordingAgeGraphClient(false),
            new SpringAiAgeProperties(true, "emme_ai_graph_", 5));
    AiExecutionContext context = context();
    GraphTraversalQuery query =
        new GraphTraversalQuery(GraphTraversalKind.DESIGN_TO_SERVICE, UUID.randomUUID(), 5);

    List<?> recommendations =
        AiExecutionContextScope.call(context, () -> adapter.retrieve(query, context));

    assertThat(recommendations).isEmpty();
  }

  @Test
  void capsTraversalResultsAtTheConfiguredTenantSafeLimit() {
    RecordingAgeGraphClient client = new RecordingAgeGraphClient(true);
    AgeGraphAdapter adapter =
        new AgeGraphAdapter(client, new SpringAiAgeProperties(true, "emme_ai_graph_", 5));
    AiExecutionContext context = context();
    GraphTraversalQuery query =
        new GraphTraversalQuery(GraphTraversalKind.DESIGN_TO_SERVICE, UUID.randomUUID(), 50);

    AiExecutionContextScope.call(context, () -> adapter.retrieve(query, context));

    assertThat(client.retrievedQuery().limit()).isEqualTo(5);
  }

  @Test
  void doesNotSilentlyDropProjectionWhenAgeIsUnavailable() {
    AgeGraphAdapter adapter =
        new AgeGraphAdapter(
            new RecordingAgeGraphClient(false),
            new SpringAiAgeProperties(true, "emme_ai_graph_", 5));
    AiExecutionContext context = context();
    GraphProjection projection =
        new GraphProjection(
            List.of(new GraphNode(GraphNodeType.DESIGN, UUID.randomUUID(), Map.of())), List.of());

    assertThatThrownBy(
            () -> AiExecutionContextScope.run(context, () -> adapter.project(projection, context)))
        .isInstanceOf(AgeGraphUnavailableException.class);
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("ROLE_CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-age",
        "idempotency-age");
  }

  private static final class RecordingAgeGraphClient implements AgeGraphClient {
    private final boolean available;
    private UUID projectedTenant;
    private String projectedGraphName;
    private GraphTraversalQuery retrievedQuery;

    private RecordingAgeGraphClient(boolean available) {
      this.available = available;
    }

    @Override
    public boolean available() {
      return available;
    }

    @Override
    public void project(
        String graphName, UUID tenantId, GraphProjection projection, Instant projectedAt) {
      projectedGraphName = graphName;
      projectedTenant = tenantId;
    }

    @Override
    public List<com.emme.ai.contracts.graph.GraphRecommendation> retrieve(
        String graphName, UUID tenantId, GraphTraversalQuery query) {
      retrievedQuery = query;
      return List.of();
    }

    UUID projectedTenant() {
      return projectedTenant;
    }

    String projectedGraphName() {
      return projectedGraphName;
    }

    GraphTraversalQuery retrievedQuery() {
      return retrievedQuery;
    }
  }
}
