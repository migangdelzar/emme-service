package com.emme.assistant.ai.adapter.out.graph;

import com.emme.ai.contracts.graph.GraphEdge;
import com.emme.ai.contracts.graph.GraphNode;
import com.emme.ai.contracts.graph.GraphNodeReference;
import com.emme.ai.contracts.graph.GraphProjection;
import com.emme.ai.contracts.graph.GraphRecommendation;
import com.emme.ai.contracts.graph.GraphTraversalKind;
import com.emme.ai.contracts.graph.GraphTraversalQuery;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

/** JDBC implementation of the fixed-query Apache AGE projection and retrieval boundary. */
public final class JdbcAgeGraphClient implements AgeGraphClient {

  private static final TypeReference<Map<String, String>> PROPERTIES_TYPE =
      new TypeReference<>() {};

  private final JdbcClient jdbc;
  private final JdbcOperations operations;
  private final ObjectMapper objectMapper;
  private final TransactionOperations transactions;

  public JdbcAgeGraphClient(
      JdbcOperations operations, ObjectMapper objectMapper, TransactionOperations transactions) {
    this.operations = Objects.requireNonNull(operations, "operations must not be null");
    this.jdbc = JdbcClient.create(operations);
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    this.transactions = Objects.requireNonNull(transactions, "transactions must not be null");
  }

  @Override
  public boolean available() {
    try {
      return jdbc.sql("SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'age')")
          .query((resultSet, rowNumber) -> resultSet.getBoolean(1))
          .single();
    } catch (DataAccessException exception) {
      return false;
    }
  }

  @Override
  public void project(
      String graphName, UUID tenantId, GraphProjection projection, Instant projectedAt) {
    Objects.requireNonNull(graphName, "graphName must not be null");
    Objects.requireNonNull(tenantId, "tenantId must not be null");
    Objects.requireNonNull(projection, "projection must not be null");
    Objects.requireNonNull(projectedAt, "projectedAt must not be null");
    transactions.execute(
        status -> {
          setAgeSearchPath();
          ensureGraph(graphName);
          projection.nodes().forEach(node -> upsertNode(graphName, tenantId, node));
          projection.edges().forEach(edge -> upsertEdge(graphName, tenantId, edge));
          updateRegistry(graphName, tenantId, projectedAt);
          return null;
        });
  }

  @Override
  public List<GraphRecommendation> retrieve(
      String graphName, UUID tenantId, GraphTraversalQuery query) {
    Objects.requireNonNull(graphName, "graphName must not be null");
    Objects.requireNonNull(tenantId, "tenantId must not be null");
    Objects.requireNonNull(query, "query must not be null");
    if (query.kind() != GraphTraversalKind.DESIGN_TO_SERVICE) {
      throw new IllegalArgumentException("unsupported graph traversal");
    }

    return transactions.execute(status -> retrieveWithinTransaction(graphName, tenantId, query));
  }

  private List<GraphRecommendation> retrieveWithinTransaction(
      String graphName, UUID tenantId, GraphTraversalQuery query) {
    setAgeSearchPath();
    List<Instant> projectionTimes =
        jdbc.sql(
                """
                SELECT last_projected_at
                FROM ai_age_graph_registry
                WHERE tenant_id = :tenantId
                  AND graph_name = :graphName
                """)
            .param("tenantId", tenantId)
            .param("graphName", graphName)
            .query(
                (resultSet, rowNumber) -> {
                  java.sql.Timestamp timestamp = resultSet.getTimestamp("last_projected_at");
                  return timestamp == null ? null : timestamp.toInstant();
                })
            .list();
    Instant projectedAt =
        projectionTimes.stream().filter(Objects::nonNull).findFirst().orElse(Instant.EPOCH);

    String cypher =
        """
        MATCH (design:Design {source_id: "%s", tenant_id: "%s"})
              -[:COMPATIBLE_WITH]->
              (service:Service {tenant_id: "%s"})
        RETURN service.source_id, service.payload_json
        LIMIT %d
        """
            .formatted(
                cypherString(query.sourceId().toString()),
                cypherString(tenantId.toString()),
                cypherString(tenantId.toString()),
                query.limit());
    String sql =
        """
        SELECT target_id, payload_json
        FROM ag_catalog.cypher(%s, %s)
            AS (target_id ag_catalog.agtype, payload_json ag_catalog.agtype)
        """
            .formatted(sqlStringLiteral(graphName), dollarQuoted(cypher));
    return operations.query(
        sql,
        (resultSet, rowNumber) ->
            recommendation(
                query.sourceId(),
                resultSet.getString("target_id"),
                resultSet.getString("payload_json"),
                projectedAt));
  }

  private void setAgeSearchPath() {
    jdbc.sql("LOAD 'age'").update();
    jdbc.sql("SET LOCAL search_path TO ag_catalog, \"$user\", public").update();
  }

  private void ensureGraph(String graphName) {
    boolean exists =
        jdbc.sql(
                "SELECT EXISTS (SELECT 1 FROM ag_catalog.ag_graph WHERE name = CAST(:graphName AS name))")
            .param("graphName", graphName)
            .query((resultSet, rowNumber) -> resultSet.getBoolean(1))
            .single();
    if (!exists) {
      jdbc.sql("SELECT ag_catalog.create_graph(CAST(:graphName AS name))")
          .param("graphName", graphName)
          .query((resultSet, rowNumber) -> resultSet.getObject(1))
          .list();
    }
  }

  private void upsertNode(String graphName, UUID tenantId, GraphNode node) {
    String payload = json(node.properties());
    String cypher =
        ("MERGE (node:%s {source_id: \"%s\", tenant_id: \"%s\"}) "
                + "SET node.payload_json = \"%s\" RETURN node")
            .formatted(
                node.type().ageLabel(),
                cypherString(node.sourceId().toString()),
                cypherString(tenantId.toString()),
                cypherString(payload));
    executeCypher(graphName, cypher);
  }

  private void upsertEdge(String graphName, UUID tenantId, GraphEdge edge) {
    GraphNodeReference source = edge.source();
    GraphNodeReference target = edge.target();
    String payload = json(edge.properties());
    String cypher =
        ("MATCH (source:%s {source_id: \"%s\", tenant_id: \"%s\"}), "
                + "(target:%s {source_id: \"%s\", tenant_id: \"%s\"}) "
                + "MERGE (source)-[relationship:%s]->(target) "
                + "SET relationship.payload_json = \"%s\" RETURN relationship")
            .formatted(
                source.type().ageLabel(),
                cypherString(source.sourceId().toString()),
                cypherString(tenantId.toString()),
                target.type().ageLabel(),
                cypherString(target.sourceId().toString()),
                cypherString(tenantId.toString()),
                edge.relationship().name(),
                cypherString(payload));
    executeCypher(graphName, cypher);
  }

  private void executeCypher(String graphName, String cypher) {
    String sql =
        """
        SELECT result
        FROM ag_catalog.cypher(%s, %s)
            AS (result ag_catalog.agtype)
        """
            .formatted(sqlStringLiteral(graphName), dollarQuoted(cypher));
    operations.query(sql, (resultSet, rowNumber) -> resultSet.getObject("result"));
  }

  private void updateRegistry(String graphName, UUID tenantId, Instant projectedAt) {
    jdbc.sql(
            """
            INSERT INTO ai_age_graph_registry
                (tenant_id, graph_name, age_enabled, projection_version, last_projected_at)
            VALUES (:tenantId, :graphName, true, 1, :projectedAt)
            ON CONFLICT (tenant_id)
            DO UPDATE SET
                graph_name = EXCLUDED.graph_name,
                age_enabled = true,
                projection_version = ai_age_graph_registry.projection_version + 1,
                last_projected_at = EXCLUDED.last_projected_at,
                updated_at = CURRENT_TIMESTAMP
            """)
        .param("tenantId", tenantId)
        .param("graphName", graphName)
        .param("projectedAt", java.sql.Timestamp.from(projectedAt))
        .update();
  }

  private GraphRecommendation recommendation(
      UUID sourceId, String targetIdValue, String payloadValue, Instant projectedAt) {
    UUID targetId = UUID.fromString(agTypeString(targetIdValue));
    try {
      return new GraphRecommendation(
          sourceId,
          targetId,
          com.emme.ai.contracts.graph.GraphNodeType.SERVICE,
          com.emme.ai.contracts.graph.GraphRelationshipType.COMPATIBLE_WITH,
          objectMapper.readValue(agTypeString(payloadValue), PROPERTIES_TYPE),
          projectedAt);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to deserialize AGE graph recommendation", exception);
    }
  }

  private String json(Map<String, String> properties) {
    try {
      return objectMapper.writeValueAsString(properties);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to serialize AGE graph properties", exception);
    }
  }

  private String agTypeString(String value) {
    String normalized = Objects.requireNonNull(value, "AGE value must not be null").trim();
    if (normalized.length() >= 2
        && normalized.charAt(0) == '"'
        && normalized.charAt(normalized.length() - 1) == '"') {
      try {
        return objectMapper.readValue(normalized, String.class);
      } catch (Exception exception) {
        throw new IllegalStateException("Unable to deserialize AGE scalar", exception);
      }
    }
    return normalized;
  }

  private static String cypherString(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static String sqlStringLiteral(String value) {
    return "'" + value.replace("'", "''") + "'";
  }

  private static String dollarQuoted(String value) {
    String tag = "$emme_ai$";
    int suffix = 0;
    while (value.contains(tag)) {
      tag = "$emme_ai_" + ++suffix + "$";
    }
    return tag + value + tag;
  }
}
