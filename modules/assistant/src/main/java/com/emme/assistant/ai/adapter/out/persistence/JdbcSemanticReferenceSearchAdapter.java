package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.assistant.ai.application.port.out.SemanticReferenceSearchPort;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import com.emme.assistant.ai.application.semantic.SemanticMatch;
import com.emme.assistant.ai.configuration.AiProperties;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** PostgreSQL pgvector adapter for tenant-scoped intent and tool reference search. */
@Component
public final class JdbcSemanticReferenceSearchAdapter implements SemanticReferenceSearchPort {

  private static final String INTENT_TABLE = "ai_intent_reference";
  private static final String TOOL_TABLE = "ai_tool_reference";

  private final JdbcClient jdbc;
  private final String embeddingModelName;
  private final int embeddingDimensions;
  private final String embeddingModelVersion;

  public JdbcSemanticReferenceSearchAdapter(JdbcClient jdbc, AiProperties aiProperties) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
    AiProperties properties = Objects.requireNonNull(aiProperties, "aiProperties must not be null");
    this.embeddingModelName = properties.embeddingModelName();
    this.embeddingDimensions = properties.embeddingDimension();
    this.embeddingModelVersion = properties.embeddingModelVersion();
  }

  @Override
  public List<SemanticMatch> searchIntents(String locale, EmbeddingVector query, int limit) {
    requireSearchArguments(locale, query, limit);
    return search(
        INTENT_TABLE,
        "intent_key",
        locale,
        query,
        limit,
        "embedding_model_version = :embeddingModelVersion");
  }

  @Override
  public List<SemanticMatch> searchTools(
      String locale, EmbeddingVector query, Set<String> authorizedToolKeys, int limit) {
    requireSearchArguments(locale, query, limit);
    Objects.requireNonNull(authorizedToolKeys, "authorizedToolKeys must not be null");
    List<String> authorized = authorizedToolKeys.stream().sorted().toList();
    if (authorized.isEmpty()) {
      return List.of();
    }

    String toolPredicate =
        "AND tool_key IN ("
            + java.util.stream.IntStream.range(0, authorized.size())
                .mapToObj(index -> ":authorizedTool" + index)
                .collect(java.util.stream.Collectors.joining(", "))
            + ")";
    var statement =
        jdbc.sql(
                semanticSearchSql(
                    TOOL_TABLE,
                    "tool_key",
                    "locale = :locale " + toolPredicate,
                    "embedding_model_version = :embeddingModelVersion"))
            .param("tenantId", AiExecutionContextScope.requireCurrent().tenantId())
            .param("locale", locale)
            .param("queryEmbedding", vectorLiteral(query))
            .param("embeddingModelName", embeddingModelName)
            .param("embeddingModelVersion", query.modelVersion())
            .param("embeddingDimension", query.values().size())
            .param("limit", limit);
    for (int index = 0; index < authorized.size(); index++) {
      statement = statement.param("authorizedTool" + index, authorized.get(index));
    }
    return statement
        .query(
            (resultSet, rowNumber) ->
                match(resultSet.getString("semantic_key"), resultSet.getDouble("similarity")))
        .list();
  }

  private List<SemanticMatch> search(
      String table,
      String keyColumn,
      String locale,
      EmbeddingVector query,
      int limit,
      String modelPredicate) {
    return jdbc.sql(semanticSearchSql(table, keyColumn, "locale = :locale", modelPredicate))
        .param("tenantId", AiExecutionContextScope.requireCurrent().tenantId())
        .param("locale", locale)
        .param("queryEmbedding", vectorLiteral(query))
        .param("embeddingModelName", embeddingModelName)
        .param("embeddingModelVersion", query.modelVersion())
        .param("embeddingDimension", query.values().size())
        .param("limit", limit)
        .query(
            (resultSet, rowNumber) ->
                match(resultSet.getString("semantic_key"), resultSet.getDouble("similarity")))
        .list();
  }

  private static String semanticSearchSql(
      String table, String keyColumn, String referencePredicate, String modelPredicate) {
    return """
    SELECT %s AS semantic_key,
           1 - (embedding <=> CAST(:queryEmbedding AS vector)) AS similarity
    FROM %s
    WHERE tenant_id = :tenantId
      AND active = true
      AND embedding IS NOT NULL
      AND vector_dims(embedding) = :embeddingDimension
      AND embedding_model_name = :embeddingModelName
      AND %s
      AND %s
    ORDER BY embedding <=> CAST(:queryEmbedding AS vector), id
    LIMIT :limit
    """
        .formatted(keyColumn, table, referencePredicate, modelPredicate);
  }

  private static SemanticMatch match(String key, double similarity) {
    return new SemanticMatch(key, similarity);
  }

  private void requireSearchArguments(String locale, EmbeddingVector query, int limit) {
    AiExecutionContextScope.requireCurrent();
    if (locale == null || locale.isBlank()) {
      throw new IllegalArgumentException("locale must not be blank");
    }
    Objects.requireNonNull(query, "query must not be null");
    if (query.values().size() != embeddingDimensions) {
      throw new IllegalArgumentException("Embedding dimensions must match pgvector schema");
    }
    if (!embeddingModelVersion.equals(query.modelVersion())) {
      throw new IllegalArgumentException("Embedding model version must match configured model");
    }
    if (limit <= 0) {
      throw new IllegalArgumentException("limit must be greater than zero");
    }
  }

  private static String vectorLiteral(EmbeddingVector vector) {
    return vector.values().toString();
  }
}
