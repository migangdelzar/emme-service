package com.emme.assistant.ai.adapter.out.workflow;

import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.bsc.langgraph4j.checkpoint.Checkpoint;
import org.springframework.jdbc.core.simple.JdbcClient;

/** PostgreSQL-backed LangGraph checkpoint adapter for the tenant-scoped Emme workflow table. */
public final class JdbcLangGraphCheckpointSaver implements BaseCheckpointSaver {

  private static final TypeReference<Map<String, Object>> STATE_TYPE = new TypeReference<>() {};

  private final JdbcClient jdbc;
  private final ObjectMapper objectMapper;
  private static final Set<String> STAFF_ROLES =
      Set.of(
          "tenant_staff",
          "tenant_owner",
          "ROLE_tenant_staff",
          "ROLE_tenant_owner",
          "ROLE_STAFF",
          "ROLE_OWNER",
          "ROLE_ADMIN",
          "ROLE_admin",
          "admin");

  public JdbcLangGraphCheckpointSaver(JdbcClient jdbc, ObjectMapper objectMapper) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public Collection<Checkpoint> list(RunnableConfig config) {
    RunnableConfig validated = TenantAwareCheckpointSaver.validateConfig(config);
    var context = AiExecutionContextScope.requireCurrent();
    WorkflowThread thread = workflowThread(validated);
    if (!isWorkflowAccessibleOrNew(context, thread.workflowId())) {
      return List.of();
    }
    return jdbc.sql(
            """
            SELECT checkpoint.node_execution_key, checkpoint.node_name,
                   checkpoint.next_node_name, checkpoint.state::text AS state
            FROM ai_workflow_checkpoint checkpoint
            JOIN ai_workflow_run workflow ON workflow.id = checkpoint.workflow_id
            WHERE checkpoint.tenant_id = :tenantId
              AND checkpoint.workflow_id = :workflowId
              AND checkpoint.workflow_namespace = :namespace
              AND workflow.conversation_id = :conversationId
              AND (workflow.principal_id = :principalId OR :staffReviewer)
            ORDER BY checkpoint.created_at DESC, checkpoint.id DESC
            """)
        .param("tenantId", context.tenantId())
        .param("workflowId", thread.workflowId())
        .param("namespace", thread.namespace())
        .param("conversationId", context.conversationId())
        .param("principalId", context.principalId())
        .param("staffReviewer", isStaffReviewer(context))
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
    WorkflowThread thread = workflowThread(validated);
    if (!isWorkflowAccessibleOrNew(context, thread.workflowId())) {
      return Optional.empty();
    }
    String checkpointId = validated.checkPointId().orElseThrow();
    List<Checkpoint> checkpoints =
        jdbc.sql(
                """
                SELECT checkpoint.node_execution_key, checkpoint.node_name,
                       checkpoint.next_node_name, checkpoint.state::text AS state
                FROM ai_workflow_checkpoint checkpoint
                JOIN ai_workflow_run workflow ON workflow.id = checkpoint.workflow_id
                WHERE checkpoint.tenant_id = :tenantId
                  AND checkpoint.workflow_id = :workflowId
                  AND checkpoint.workflow_namespace = :namespace
                  AND checkpoint.node_execution_key = :checkpointId
                  AND workflow.conversation_id = :conversationId
                  AND (workflow.principal_id = :principalId OR :staffReviewer)
                ORDER BY checkpoint.created_at DESC, checkpoint.id DESC
                """)
            .param("tenantId", context.tenantId())
            .param("workflowId", thread.workflowId())
            .param("namespace", thread.namespace())
            .param("checkpointId", checkpointId)
            .param("conversationId", context.conversationId())
            .param("principalId", context.principalId())
            .param("staffReviewer", isStaffReviewer(context))
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
    WorkflowThread thread = workflowThread(validated);
    ensureWorkflowRun(context, thread.workflowId());
    String state = objectMapper.writeValueAsString(checkpoint.getState());
    jdbc.sql(
            """
            INSERT INTO ai_workflow_checkpoint (
                tenant_id,
                workflow_id,
                workflow_namespace,
                node_name,
                node_execution_key,
                next_node_name,
                state
            )
            VALUES (
                :tenantId,
                :workflowId,
                :namespace,
                :nodeName,
                :nodeExecutionKey,
                :nextNodeName,
                CAST(:state AS jsonb)
            )
            ON CONFLICT (tenant_id, workflow_id, workflow_namespace, node_name, node_execution_key)
            DO UPDATE SET
                next_node_name = EXCLUDED.next_node_name,
                state = EXCLUDED.state,
                updated_at = CURRENT_TIMESTAMP,
                version = ai_workflow_checkpoint.version + 1
            RETURNING node_execution_key
            """)
        .param("tenantId", context.tenantId())
        .param("workflowId", thread.workflowId())
        .param("namespace", thread.namespace())
        .param("nodeName", checkpoint.getNodeId())
        .param("nodeExecutionKey", checkpoint.getId())
        .param("nextNodeName", checkpoint.getNextNodeId())
        .param("state", state)
        .query((resultSet, rowNumber) -> resultSet.getString("node_execution_key"))
        .single();

    updateWorkflowRun(context, thread.workflowId(), state);

    return RunnableConfig.builder(validated).checkPointId(checkpoint.getId()).build();
  }

  private void ensureWorkflowRun(
      com.emme.kernel.context.AiExecutionContext context, UUID workflowId) {
    jdbc.sql(
            """
            INSERT INTO ai_workflow_run (
                id,
                tenant_id,
                principal_id,
                conversation_id,
                workflow_type,
                status,
                graph_version,
                idempotency_key,
                state
            )
            VALUES (
                :workflowId,
                :tenantId,
                :principalId,
                :conversationId,
                'CONVERSATION',
                'RECEIVED',
                'conversation-v1',
                :idempotencyKey,
                CAST('{}' AS jsonb)
            )
            ON CONFLICT (id) DO NOTHING
            """)
        .param("workflowId", workflowId)
        .param("tenantId", context.tenantId())
        .param("principalId", context.principalId())
        .param("conversationId", context.conversationId())
        .param("idempotencyKey", context.idempotencyKey())
        .update();
    Integer matchingRun =
        jdbc.sql(
                """
                SELECT COUNT(*)
                FROM ai_workflow_run
                WHERE id = :workflowId
                  AND tenant_id = :tenantId
                  AND conversation_id = :conversationId
                  AND (principal_id = :principalId OR :staffReviewer)
                """)
            .param("workflowId", workflowId)
            .param("tenantId", context.tenantId())
            .param("principalId", context.principalId())
            .param("conversationId", context.conversationId())
            .param("staffReviewer", isStaffReviewer(context))
            .query(Integer.class)
            .single();
    if (matchingRun == null || matchingRun != 1) {
      throw new SecurityException("Workflow run is not accessible for the authenticated context");
    }
  }

  private boolean isWorkflowAccessibleOrNew(
      com.emme.kernel.context.AiExecutionContext context, UUID workflowId) {
    Integer matchingRun =
        jdbc.sql(
                """
                SELECT COUNT(*)
                FROM ai_workflow_run
                WHERE id = :workflowId
                  AND tenant_id = :tenantId
                  AND conversation_id = :conversationId
                  AND (principal_id = :principalId OR :staffReviewer)
                """)
            .param("workflowId", workflowId)
            .param("tenantId", context.tenantId())
            .param("conversationId", context.conversationId())
            .param("principalId", context.principalId())
            .param("staffReviewer", isStaffReviewer(context))
            .query(Integer.class)
            .single();
    if (matchingRun != null && matchingRun == 1) {
      return true;
    }
    Integer existingRun =
        jdbc.sql(
                """
                SELECT COUNT(*)
                FROM ai_workflow_run
                WHERE id = :workflowId
                  AND tenant_id = :tenantId
                  AND conversation_id = :conversationId
                """)
            .param("workflowId", workflowId)
            .param("tenantId", context.tenantId())
            .param("conversationId", context.conversationId())
            .query(Integer.class)
            .single();
    if (existingRun == null || existingRun == 0) {
      return false;
    }
    throw new SecurityException("Workflow run is not accessible for the authenticated context");
  }

  private void updateWorkflowRun(
      com.emme.kernel.context.AiExecutionContext context, UUID workflowId, String state) {
    try {
      Map<String, Object> values = objectMapper.readValue(state, STATE_TYPE);
      String status = values.get("status") instanceof String value ? value : "RUNNING";
      jdbc.sql(
              """
              UPDATE ai_workflow_run
              SET status = :status,
                  state = CAST(:state AS jsonb),
                  updated_at = CURRENT_TIMESTAMP,
                  version = version + 1
              WHERE id = :workflowId
                AND tenant_id = :tenantId
                AND conversation_id = :conversationId
                AND (principal_id = :principalId OR :staffReviewer)
              """)
          .param("status", status)
          .param("state", state)
          .param("workflowId", workflowId)
          .param("tenantId", context.tenantId())
          .param("conversationId", context.conversationId())
          .param("principalId", context.principalId())
          .param("staffReviewer", isStaffReviewer(context))
          .update();
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to update workflow run progress", exception);
    }
  }

  private static WorkflowThread workflowThread(RunnableConfig config) {
    String threadId = config.threadId().orElseThrow();
    int separator = threadId.indexOf(':');
    String workflowId = separator < 0 ? threadId : threadId.substring(0, separator);
    String namespace = separator < 0 ? "default" : threadId.substring(separator + 1);
    if (namespace.isBlank()) {
      throw new IllegalArgumentException("Checkpoint thread namespace must not be blank");
    }
    if (namespace.indexOf(':') >= 0) {
      throw new IllegalArgumentException("Checkpoint thread namespace must not contain ':'");
    }
    try {
      return new WorkflowThread(UUID.fromString(workflowId), namespace);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(
          "Checkpoint thread must start with a workflow UUID", exception);
    }
  }

  private static boolean isStaffReviewer(com.emme.kernel.context.AiExecutionContext context) {
    return context.roles().stream().anyMatch(STAFF_ROLES::contains);
  }

  private record WorkflowThread(UUID workflowId, String namespace) {}

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
