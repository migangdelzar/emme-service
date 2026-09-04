package com.emme.assistant.ai.adapter.out.persistence;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class JdbcDesignImageMetadataRepositoryTest {

  @Test
  void savesMetadataWithTheTenantAndWorkflowScope() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), org.mockito.ArgumentMatchers.<Object>any()))
        .thenReturn(statement);
    when(statement.update()).thenReturn(1);
    JdbcDesignImageMetadataRepository repository = new JdbcDesignImageMetadataRepository(jdbc);
    UUID tenantId = UUID.randomUUID();
    UUID workflowId = UUID.randomUUID();

    repository.save(tenantId, workflowId, "design.png", "image/png", 128L);

    verify(statement).param("tenantId", tenantId);
    verify(statement).param("workflowId", workflowId);
    verify(statement).param("storageKey", "design.png");
    verify(statement).param("sizeBytes", 128L);
  }

  @Test
  void deletesMetadataWithTheTenantWorkflowAndStorageKeyScope() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), org.mockito.ArgumentMatchers.<Object>any()))
        .thenReturn(statement);
    when(statement.update()).thenReturn(1);
    JdbcDesignImageMetadataRepository repository = new JdbcDesignImageMetadataRepository(jdbc);
    UUID tenantId = UUID.randomUUID();
    UUID workflowId = UUID.randomUUID();

    repository.delete(tenantId, workflowId, "design.png");

    verify(statement).param(eq("tenantId"), eq(tenantId));
    verify(statement).param(eq("workflowId"), eq(workflowId));
    verify(statement).param(eq("storageKey"), eq("design.png"));
  }
}
