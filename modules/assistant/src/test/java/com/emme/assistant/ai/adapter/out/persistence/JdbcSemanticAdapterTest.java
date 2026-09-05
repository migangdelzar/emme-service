package com.emme.assistant.ai.adapter.out.persistence;

import static com.emme.assistant.ai.EmbeddingTestVectors.testEmbedding;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.semantic.EmbeddingModelDefaults;
import com.emme.ai.contracts.semantic.EmbeddingVector;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.semantic.SemanticCacheIdentity;
import com.emme.assistant.ai.application.semantic.SemanticMatch;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

@SuppressWarnings("unchecked")
class JdbcSemanticAdapterTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();
  private static final EmbeddingVector QUERY =
      testEmbedding(EmbeddingModelDefaults.MODEL_VERSION, java.util.Collections.nCopies(768, 0.0f));

  @Test
  void referenceSearchUsesAuthenticatedTenantAndEmbeddingVersion() throws Exception {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<SemanticMatch> result = mock(JdbcClient.MappedQuerySpec.class);
    stubQuery(jdbc, statement, result);
    when(result.list()).thenReturn(List.of());
    JdbcSemanticReferenceSearchAdapter adapter =
        new JdbcSemanticReferenceSearchAdapter(jdbc, aiProperties());

    AiExecutionContext context = context();
    AiExecutionContextScope.call(context, () -> adapter.searchIntents("es-MX", QUERY, 2));

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("tenant_id = :tenantId")
        .contains("embedding_model_version = :embeddingModelVersion")
        .contains("embedding_model_name = :embeddingModelName")
        .contains("vector_dims(embedding) = :embeddingDimension")
        .contains("ORDER BY embedding <=> CAST(:queryEmbedding AS vector)");
    verify(statement).param("tenantId", TENANT_ID);
    verify(statement).param("embeddingModelVersion", EmbeddingModelDefaults.MODEL_VERSION);
    verify(statement).param("embeddingModelName", "embeddinggemma:300m");
    verify(statement).param("embeddingDimension", 768);
  }

  @Test
  void toolSearchBindsOnlyTheBackendAuthorizedToolKeys() throws Exception {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<SemanticMatch> result = mock(JdbcClient.MappedQuerySpec.class);
    stubQuery(jdbc, statement, result);
    when(result.list()).thenReturn(List.of());
    JdbcSemanticReferenceSearchAdapter adapter =
        new JdbcSemanticReferenceSearchAdapter(jdbc, aiProperties());

    AiExecutionContextScope.call(
        context(), () -> adapter.searchTools("es-MX", QUERY, Set.of("findAvailability"), 2));

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue()).contains("tool_key IN (:authorizedTool0)");
    verify(statement).param("authorizedTool0", "findAvailability");
  }

  @Test
  void cacheSearchIsPrincipalScopedAndExcludesExpiredRows() throws Exception {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<SemanticCachePort.Candidate> result =
        mock(JdbcClient.MappedQuerySpec.class);
    stubQuery(jdbc, statement, result);
    when(result.list()).thenReturn(List.of());
    JdbcSemanticCacheAdapter adapter = new JdbcSemanticCacheAdapter(jdbc, aiProperties());

    SemanticCacheIdentity identity =
        new SemanticCacheIdentity(
            "ollama", "gemma4:e4b-mlx", "knowledge-v7", "policy-v3", "source-v9");
    SemanticCachePort.Lookup lookup =
        new SemanticCachePort.Lookup("FAQ", "catalog-v4", "prompt-v2", QUERY, identity);
    AiExecutionContextScope.call(context(), () -> adapter.find(lookup, 2));

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("tenant_id = :tenantId")
        .contains("principal_id = :principalId")
        .contains("embedding_model_name = :embeddingModelName")
        .contains("response_provider = :responseProvider")
        .contains("response_model = :responseModel")
        .contains("knowledge_version = :knowledgeVersion")
        .contains("policy_version = :policyVersion")
        .contains("source_version = :sourceVersion")
        .contains("expires_at > CURRENT_TIMESTAMP");
    verify(statement).param("tenantId", TENANT_ID);
    verify(statement).param("principalId", PRINCIPAL_ID);
    verify(statement).param("embeddingModelName", "embeddinggemma:300m");
    verify(statement).param("responseProvider", identity.responseProvider());
    verify(statement).param("responseModel", identity.responseModel());
    verify(statement).param("knowledgeVersion", identity.knowledgeVersion());
    verify(statement).param("policyVersion", identity.policyVersion());
    verify(statement).param("sourceVersion", identity.sourceVersion());
  }

  @Test
  void cacheWriteUsesAuthenticatedScopeAndAnIdempotencyKey() throws Exception {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<UUID> result = mock(JdbcClient.MappedQuerySpec.class);
    stubQuery(jdbc, statement, result);
    when(result.single()).thenReturn(UUID.randomUUID());
    JdbcSemanticCacheAdapter adapter = new JdbcSemanticCacheAdapter(jdbc, aiProperties());

    SemanticCacheIdentity identity =
        new SemanticCacheIdentity(
            "ollama", "gemma4:e4b-mlx", "knowledge-v7", "policy-v3", "source-v9");
    SemanticCachePort.Put write =
        new SemanticCachePort.Put(
            "FAQ",
            "What are your hours?",
            "catalog-v4",
            "prompt-v2",
            "{\"answer\":\"We are open\"}",
            Instant.parse("2030-01-01T00:00:00Z"),
            QUERY,
            "cache-write-1",
            identity);

    AiExecutionContextScope.call(context(), () -> adapter.put(write));

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("INSERT INTO ai_semantic_cache")
        .contains("embedding_model_name")
        .contains("response_provider")
        .contains("response_model")
        .contains("knowledge_version")
        .contains("policy_version")
        .contains("source_version")
        .contains("write_idempotency_key")
        .contains("ON CONFLICT (tenant_id, principal_id, write_idempotency_key)")
        .contains("RETURNING id");
    verify(statement).param("tenantId", TENANT_ID);
    verify(statement).param("principalId", PRINCIPAL_ID);
    verify(statement).param("embeddingModelName", "embeddinggemma:300m");
    verify(statement).param("writeIdempotencyKey", "cache-write-1");
    verify(statement).param("responseProvider", identity.responseProvider());
    verify(statement).param("responseModel", identity.responseModel());
    verify(statement).param("knowledgeVersion", identity.knowledgeVersion());
    verify(statement).param("policyVersion", identity.policyVersion());
    verify(statement).param("sourceVersion", identity.sourceVersion());
    verify(statement).param("expiresAt", Timestamp.from(write.expiresAt()));
  }

  @Test
  void refreshesAnExpiredRowWhenAnIdempotentWriteConflicts() throws Exception {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<UUID> result = mock(JdbcClient.MappedQuerySpec.class);
    stubQuery(jdbc, statement, result);
    when(result.single()).thenReturn(UUID.randomUUID());
    JdbcSemanticCacheAdapter adapter = new JdbcSemanticCacheAdapter(jdbc, aiProperties());

    SemanticCachePort.Put write =
        new SemanticCachePort.Put(
            "FAQ",
            "What are your hours?",
            "catalog-v4",
            "prompt-v2",
            "{\"answer\":\"We are open\"}",
            Instant.parse("2030-01-01T00:00:00Z"),
            QUERY,
            "cache-write-refresh");

    AiExecutionContextScope.call(context(), () -> adapter.put(write));

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("DO UPDATE SET")
        .contains("active = true")
        .contains("expires_at = EXCLUDED.expires_at")
        .contains("response_payload = EXCLUDED.response_payload")
        .contains("embedding = EXCLUDED.embedding")
        .contains("updated_at = CURRENT_TIMESTAMP");
  }

  @Test
  void cacheHitUpdateIsTenantAndPrincipalScoped() throws Exception {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.update()).thenReturn(1);
    JdbcSemanticCacheAdapter adapter = new JdbcSemanticCacheAdapter(jdbc, aiProperties());
    UUID cacheId = UUID.randomUUID();

    boolean updated = AiExecutionContextScope.call(context(), () -> adapter.recordHit(cacheId));

    assertThat(updated).isTrue();
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("UPDATE ai_semantic_cache")
        .contains("tenant_id = :tenantId")
        .contains("principal_id = :principalId")
        .contains("hit_count = hit_count + 1")
        .contains("expires_at > CURRENT_TIMESTAMP");
    verify(statement).param("tenantId", TENANT_ID);
    verify(statement).param("principalId", PRINCIPAL_ID);
    verify(statement).param("cacheId", cacheId);
  }

  @Test
  void adaptersFailClosedWithoutAnAiExecutionContext() {
    JdbcSemanticReferenceSearchAdapter references =
        new JdbcSemanticReferenceSearchAdapter(mock(JdbcClient.class), aiProperties());
    JdbcSemanticCacheAdapter cache =
        new JdbcSemanticCacheAdapter(mock(JdbcClient.class), aiProperties());

    assertThatThrownBy(() -> references.searchIntents("es-MX", QUERY, 2))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
    assertThatThrownBy(
            () ->
                cache.find(
                    new SemanticCachePort.Lookup("FAQ", "catalog-v4", "prompt-v2", QUERY), 2))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  @Test
  void referenceSearchRejectsAQueryFromADifferentEmbeddingModel() {
    JdbcSemanticReferenceSearchAdapter adapter =
        new JdbcSemanticReferenceSearchAdapter(mock(JdbcClient.class), aiProperties());

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context(),
                    () ->
                        adapter.searchIntents(
                            "es-MX",
                            testEmbedding("other-model", java.util.Collections.nCopies(768, 0.0f)),
                            2)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Embedding model version must match configured model");
  }

  @Test
  void cacheWriteRejectsAQueryFromADifferentEmbeddingModel() {
    JdbcSemanticCacheAdapter adapter =
        new JdbcSemanticCacheAdapter(mock(JdbcClient.class), aiProperties());
    SemanticCachePort.Put write =
        new SemanticCachePort.Put(
            "FAQ",
            "What are your hours?",
            "catalog-v4",
            "prompt-v2",
            "{\"answer\":\"We are open\"}",
            Instant.parse("2030-01-01T00:00:00Z"),
            testEmbedding("other-model", java.util.Collections.nCopies(768, 0.0f)),
            "cache-write-2");

    assertThatThrownBy(() -> AiExecutionContextScope.call(context(), () -> adapter.put(write)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Embedding model version must match configured model");
  }

  @Test
  void invalidatesOnlyTheAuthenticatedTenantAndPrincipalCacheRows() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.update()).thenReturn(2);
    JdbcSemanticCacheAdapter adapter = new JdbcSemanticCacheAdapter(jdbc, aiProperties());

    AiExecutionContextScope.run(context(), () -> adapter.invalidate("FAQ"));

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("UPDATE ai_semantic_cache")
        .contains("active = false")
        .contains("tenant_id = :tenantId")
        .contains("principal_id = :principalId")
        .contains("cache_kind = :cacheKind");
    verify(statement).param("tenantId", TENANT_ID);
    verify(statement).param("principalId", PRINCIPAL_ID);
    verify(statement).param("cacheKind", "FAQ");
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        TENANT_ID,
        PRINCIPAL_ID,
        Set.of("ROLE_CLIENT"),
        CONVERSATION_ID,
        WORKFLOW_ID,
        "trace-1",
        "idem-1");
  }

  private static AiProviderProperties aiProperties() {
    return new AiProviderProperties(
        "mock",
        null,
        new AiProviderProperties.EmbeddingConfig(
            "embeddinggemma:300m", "http://localhost:11434", null, 768),
        true);
  }

  private static void stubQuery(
      JdbcClient jdbc, JdbcClient.StatementSpec statement, JdbcClient.MappedQuerySpec<?> result) {
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.query(any(RowMapper.class))).thenReturn(result);
  }
}
