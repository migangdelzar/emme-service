package com.emme.assistant.ai.adapter.out.workflow;

import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.bsc.langgraph4j.checkpoint.Checkpoint;
import org.springframework.jdbc.core.simple.JdbcClient;

/** PostgreSQL-backed LangGraph checkpoint adapter for the tenant-scoped Emme workflow table. */
public final class JdbcLangGraphCheckpointSaver implements BaseCheckpointSaver {

  private static final TypeReference<Map<String, Object>> STATE_TYPE = new TypeReference<>() {};

  private final JdbcClient jdbc;
  private final ObjectMapper objectMapper;

  public JdbcLangGraphCheckpointSaver(JdbcClient jdbc, ObjectMapper objectMapper) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public Collection<Checkpoint> list(RunnableConfig config) {
    RunnableConfig validated = TenantAwareCheckpointSaver.validateConfig(config);
    var context = AiExecutionContextScope.requireCurrent();
    return jdbc.sql(
            """
            SELECT node_execution_key, node_name, next_node_name, state::text AS state
            FROM ai_workflow_checkpoint
            WHERE tenant_id = :tenantId
              AND workflow_id = :workflowId
            ORDER BY created_at DESC, id DESC
            """)
        .param("tenantId", context.tenantId())
        .param("workflowId", context.workflowId())
        .query(this::checkpointFromRow)
        .list();
  }

  @Override
  public Optional<Checkpoint> get(RunnableConfig config) {
    RunnableConfig validated = TenantAwareCheckpointSaver.validateConfig(config);
    if (validated.checkPointId().isEmpty()) {
      return list(validated).stream().findFirst();
    }
    var context = AiExecutionContextScope.requireCurrent();
    String checkpointId = validated.checkPointId().orElseThrow();
    List<Checkpoint> checkpoints =
        jdbc.sql(
                """
                SELECT node_execution_key, node_name, next_node_name, state::text AS state
                FROM ai_workflow_checkpoint
                WHERE tenant_id = :tenantId
                  AND workflow_id = :workflowId
                  AND node_execution_key = :checkpointId
                ORDER BY created_at DESC, id DESC
                """)
            .param("tenantId", context.tenantId())
            .param("workflowId", context.workflowId())
            .param("checkpointId", checkpointId)
            .query(this::checkpointFromRow)
            .list();
    return checkpoints.stream().findFirst();
  }

  @Override
  public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
    RunnableConfig validated = TenantAwareCheckpointSaver.validateConfig(config);
    Objects.requireNonNull(checkpoint, "checkpoint must not be null");
    requireText(checkpoint.getId(), "checkpoint id");
    requireText(checkpoint.getNodeId(), "checkpoint node");

    var context = AiExecutionContextScope.requireCurrent();
    jdbc.sql(
            """
            INSERT INTO ai_workflow_checkpoint (
                tenant_id,
                workflow_id,
                node_name,
                node_execution_key,
                next_node_name,
                state
            )
            VALUES (
                :tenantId,
                :workflowId,
                :nodeName,
                :nodeExecutionKey,
                :nextNodeName,
                CAST(:state AS jsonb)
            )
            ON CONFLICT (tenant_id, workflow_id, node_name, node_execution_key)
            DO UPDATE SET
                next_node_name = EXCLUDED.next_node_name,
                state = EXCLUDED.state,
                updated_at = CURRENT_TIMESTAMP,
                version = ai_workflow_checkpoint.version + 1
            RETURNING node_execution_key
            """)
        .param("tenantId", context.tenantId())
        .param("workflowId", context.workflowId())
        .param("nodeName", checkpoint.getNodeId())
        .param("nodeExecutionKey", checkpoint.getId())
        .param("nextNodeName", checkpoint.getNextNodeId())
        .param("state", objectMapper.writeValueAsString(checkpoint.getState()))
        .query((resultSet, rowNumber) -> resultSet.getString("node_execution_key"))
        .single();

    return RunnableConfig.builder(validated).checkPointId(checkpoint.getId()).build();
  }

  @Override
  public Tag release(RunnableConfig config) throws Exception {
    RunnableConfig validated = TenantAwareCheckpointSaver.validateConfig(config);
    return new Tag(validated.threadId().orElseThrow(), list(validated));
  }

  private Checkpoint checkpointFromRow(java.sql.ResultSet resultSet, int rowNumber) {
    try {
      return Checkpoint.builder()
          .id(resultSet.getString("node_execution_key"))
          .nodeId(resultSet.getString("node_name"))
          .nextNodeId(resultSet.getString("next_node_name"))
          .state(objectMapper.readValue(resultSet.getString("state"), STATE_TYPE))
          .build();
    } catch (Exception exception) {
      throw new IllegalStateException(
          "Unable to deserialize LangGraph checkpoint state", exception);
    }
  }

  private static void requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }
}
