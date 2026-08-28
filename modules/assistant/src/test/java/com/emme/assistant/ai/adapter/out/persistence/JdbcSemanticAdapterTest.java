package com.emme.assistant.ai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.service.EmbeddingVector;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
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
      new EmbeddingVector("embedding-v1", java.util.Collections.nCopies(1024, 0.0f));

  @Test
  void referenceSearchUsesAuthenticatedTenantAndEmbeddingVersion() throws Exception {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<com.emme.assistant.ai.application.service.SemanticMatch> result =
        mock(JdbcClient.MappedQuerySpec.class);
    stubQuery(jdbc, statement, result);
    when(result.list()).thenReturn(List.of());
    JdbcSemanticReferenceSearchAdapter adapter = new JdbcSemanticReferenceSearchAdapter(jdbc);

    AiExecutionContext context = context();
    AiExecutionContextScope.call(context, () -> adapter.searchIntents("es-MX", QUERY, 2));

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("tenant_id = :tenantId")
        .contains("embedding_model_version = :embeddingModelVersion")
        .contains("ORDER BY embedding <=> CAST(:queryEmbedding AS vector)");
    verify(statement).param("tenantId", TENANT_ID);
    verify(statement).param("embeddingModelVersion", "embedding-v1");
  }

  @Test
  void toolSearchBindsOnlyTheBackendAuthorizedToolKeys() throws Exception {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<com.emme.assistant.ai.application.service.SemanticMatch> result =
        mock(JdbcClient.MappedQuerySpec.class);
    stubQuery(jdbc, statement, result);
    when(result.list()).thenReturn(List.of());
    JdbcSemanticReferenceSearchAdapter adapter = new JdbcSemanticReferenceSearchAdapter(jdbc);

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
    JdbcSemanticCacheAdapter adapter = new JdbcSemanticCacheAdapter(jdbc);

    SemanticCachePort.Lookup lookup =
        new SemanticCachePort.Lookup("FAQ", "catalog-v4", "prompt-v2", QUERY);
    AiExecutionContextScope.call(context(), () -> adapter.find(lookup, 2));

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("tenant_id = :tenantId")
        .contains("principal_id = :principalId")
        .contains("expires_at > CURRENT_TIMESTAMP");
    verify(statement).param("tenantId", TENANT_ID);
    verify(statement).param("principalId", PRINCIPAL_ID);
  }

  @Test
  void adaptersFailClosedWithoutAnAiExecutionContext() {
    JdbcSemanticReferenceSearchAdapter references =
        new JdbcSemanticReferenceSearchAdapter(mock(JdbcClient.class));
    JdbcSemanticCacheAdapter cache = new JdbcSemanticCacheAdapter(mock(JdbcClient.class));

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

  private static void stubQuery(
      JdbcClient jdbc, JdbcClient.StatementSpec statement, JdbcClient.MappedQuerySpec<?> result) {
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.query(any(RowMapper.class))).thenReturn(result);
  }
}
