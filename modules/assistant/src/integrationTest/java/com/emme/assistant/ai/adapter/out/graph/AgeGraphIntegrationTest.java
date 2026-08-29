package com.emme.assistant.ai.adapter.out.graph;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.graph.GraphEdge;
import com.emme.ai.contracts.graph.GraphNode;
import com.emme.ai.contracts.graph.GraphNodeReference;
import com.emme.ai.contracts.graph.GraphNodeType;
import com.emme.ai.contracts.graph.GraphProjection;
import com.emme.ai.contracts.graph.GraphRelationshipType;
import com.emme.ai.contracts.graph.GraphTraversalKind;
import com.emme.ai.contracts.graph.GraphTraversalQuery;
import com.emme.assistant.ai.configuration.SpringAiAgeProperties;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Apache AGE graph integration")
class AgeGraphIntegrationTest {

  private static final String IMAGE = "apache/age:release_PG17_1.6.0";

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse(IMAGE).asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("emme_test")
          .withUsername("emme")
          .withPassword("emme");

  private JdbcTemplate jdbc;
  private TransactionTemplate transactions;
  private AgeGraphAdapter adapter;

  @BeforeAll
  void connectToContainer() {
    DataSource dataSource =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    jdbc = new JdbcTemplate(dataSource);
    transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    adapter =
        new AgeGraphAdapter(
            new JdbcAgeGraphClient(jdbc, new ObjectMapper(), transactions),
            new SpringAiAgeProperties(true, "emme_ai_graph_", 5));
  }

  @BeforeEach
  void createRegistry() {
    jdbc.execute(
        """
        CREATE TABLE IF NOT EXISTS ai_age_graph_registry (
            tenant_id UUID PRIMARY KEY,
            graph_name VARCHAR(100) NOT NULL UNIQUE,
            age_enabled BOOLEAN NOT NULL DEFAULT false,
            projection_version BIGINT NOT NULL DEFAULT 0,
            last_projected_at TIMESTAMPTZ,
            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
        )
        """);
  }

  @Test
  void appliesTheAgeSearchPathInsideTheJdbcTransaction() {
    String searchPath =
        transactions.execute(
            status -> {
              jdbc.execute("SET LOCAL search_path TO ag_catalog, \"$user\", public");
              return jdbc.queryForObject("SELECT current_setting('search_path')", String.class);
            });
    assertThat(searchPath).contains("ag_catalog");
  }

  @Test
  void projectsIdempotentlyAndRetrievesOnlyTheCuratedDesignToServicePath() {
    AiExecutionContext context = context();
    UUID designId = UUID.randomUUID();
    UUID serviceId = UUID.randomUUID();
    GraphProjection projection =
        new GraphProjection(
            List.of(
                new GraphNode(GraphNodeType.DESIGN, designId, Map.of("name", "floral")),
                new GraphNode(GraphNodeType.SERVICE, serviceId, Map.of("name", "gel"))),
            List.of(
                new GraphEdge(
                    GraphRelationshipType.COMPATIBLE_WITH,
                    new GraphNodeReference(GraphNodeType.DESIGN, designId),
                    new GraphNodeReference(GraphNodeType.SERVICE, serviceId),
                    Map.of("confidence", "high"))));

    AiExecutionContextScope.run(context, () -> adapter.project(projection, context));
    AiExecutionContextScope.run(context, () -> adapter.project(projection, context));
    var recommendations =
        AiExecutionContextScope.call(
            context,
            () ->
                adapter.retrieve(
                    new GraphTraversalQuery(GraphTraversalKind.DESIGN_TO_SERVICE, designId, 5),
                    context));

    assertThat(recommendations).hasSize(1);
    assertThat(recommendations.getFirst().targetId()).isEqualTo(serviceId);
    assertThat(recommendations.getFirst().properties()).containsEntry("name", "gel");
    assertThat(recommendations.getFirst().projectedAt()).isNotEqualTo(java.time.Instant.EPOCH);
    assertThat(
            jdbc.queryForObject(
                "SELECT projection_version FROM ai_age_graph_registry WHERE tenant_id = ?",
                Long.class,
                context.tenantId()))
        .isEqualTo(2L);
  }

  @Test
  void isolatesTenantsByUsingBackendDerivedGraphNames() {
    AiExecutionContext first = context();
    AiExecutionContext second = context();
    UUID designId = UUID.randomUUID();
    UUID firstServiceId = UUID.randomUUID();
    UUID secondServiceId = UUID.randomUUID();

    AiExecutionContextScope.run(
        first, () -> adapter.project(projection(designId, firstServiceId, "first-tenant"), first));
    AiExecutionContextScope.run(
        second,
        () -> adapter.project(projection(designId, secondServiceId, "second-tenant"), second));

    var firstRecommendations =
        AiExecutionContextScope.call(
            first,
            () ->
                adapter.retrieve(
                    new GraphTraversalQuery(GraphTraversalKind.DESIGN_TO_SERVICE, designId, 5),
                    first));
    var secondRecommendations =
        AiExecutionContextScope.call(
            second,
            () ->
                adapter.retrieve(
                    new GraphTraversalQuery(GraphTraversalKind.DESIGN_TO_SERVICE, designId, 5),
                    second));

    assertThat(firstRecommendations).extracting("targetId").containsExactly(firstServiceId);
    assertThat(secondRecommendations).extracting("targetId").containsExactly(secondServiceId);
  }

  private static GraphProjection projection(UUID designId, UUID serviceId, String name) {
    return new GraphProjection(
        List.of(
            new GraphNode(GraphNodeType.DESIGN, designId, Map.of("name", name)),
            new GraphNode(GraphNodeType.SERVICE, serviceId, Map.of("name", name))),
        List.of(
            new GraphEdge(
                GraphRelationshipType.COMPATIBLE_WITH,
                new GraphNodeReference(GraphNodeType.DESIGN, designId),
                new GraphNodeReference(GraphNodeType.SERVICE, serviceId),
                Map.of())));
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("ROLE_CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-age-integration",
        "idempotency-age-integration");
  }
}
