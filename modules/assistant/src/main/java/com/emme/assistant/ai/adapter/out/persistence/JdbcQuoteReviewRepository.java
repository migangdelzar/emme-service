package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.assistant.ai.application.port.out.QuoteReviewRepository;
import com.emme.assistant.ai.domain.workflow.QuoteReviewDecisionType;
import com.emme.assistant.ai.domain.workflow.QuoteReviewStatus;
import com.emme.assistant.ai.domain.workflow.QuoteReviewTask;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** PostgreSQL adapter for tenant-scoped quote review lookup and resolution. */
@Component
public class JdbcQuoteReviewRepository implements QuoteReviewRepository {

  private static final TypeReference<List<String>> REASONS_TYPE = new TypeReference<>() {};

  private final JdbcClient jdbc;
  private final ObjectMapper objectMapper;

  public JdbcQuoteReviewRepository(JdbcClient jdbc, ObjectMapper objectMapper) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public Optional<QuoteReviewTask> findById(UUID reviewTaskId) {
    Objects.requireNonNull(reviewTaskId, "reviewTaskId must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    return jdbc
        .sql(
            """
            SELECT review_task.id,
                   review_task.tenant_id,
                   review_task.workflow_id,
                   review_task.status,
                   review_task.reviewer_id,
                   review_task.uncertainty_reasons::text AS uncertainty_reasons,
                   review_task.version,
                   decision.decision,
                   decision.notes
            FROM quote_review_task review_task
            LEFT JOIN LATERAL (
                SELECT decision, notes
                FROM quote_review_decision
                WHERE tenant_id = :tenantId
                  AND review_task_id = review_task.id
                ORDER BY decision_version DESC
                LIMIT 1
            ) decision ON true
            WHERE review_task.id = :reviewTaskId
              AND review_task.tenant_id = :tenantId
            """)
        .param("tenantId", context.tenantId())
        .param("reviewTaskId", reviewTaskId)
        .query((resultSet, rowNumber) -> reviewTaskFromRow(resultSet, context))
        .list()
        .stream()
        .findFirst();
  }

  @Override
  @Transactional
  public QuoteReviewTask save(QuoteReviewTask reviewTask) {
    Objects.requireNonNull(reviewTask, "reviewTask must not be null");
    AiExecutionContext context = requireContext(reviewTask);
    if (reviewTask.decision().isEmpty() || reviewTask.reviewerId() == null) {
      throw new IllegalArgumentException("Only resolved quote reviews can be persisted");
    }
    if (!context.principalId().equals(reviewTask.reviewerId())) {
      throw new IllegalArgumentException("reviewerId does not match AI execution context");
    }
    long expectedVersion = reviewTask.version() - 1;
    if (expectedVersion < 0) {
      throw new IllegalArgumentException("Resolved quote review version must be positive");
    }

    int updated =
        jdbc.sql(
                """
                UPDATE quote_review_task
                SET status = :status,
                    reviewer_id = :reviewerId,
                    resolved_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = :reviewTaskId
                  AND tenant_id = :tenantId
                  AND workflow_id = :workflowId
                  AND version = :expectedVersion
                  AND status IN ('WAITING_FOR_STAFF', 'CLAIMED')
                """)
            .param("status", reviewTask.status().name())
            .param("reviewerId", reviewTask.reviewerId())
            .param("reviewTaskId", reviewTask.id())
            .param("tenantId", context.tenantId())
            .param("workflowId", reviewTask.workflowId())
            .param("expectedVersion", expectedVersion)
            .update();
    if (updated != 1) {
      throw new com.emme.assistant.ai.domain.workflow.StaleQuoteReviewVersionException(
          expectedVersion, expectedVersion + 1);
    }

    jdbc.sql(
            """
            INSERT INTO quote_review_decision (
                tenant_id, review_task_id, reviewer_id, decision_version,
                decision, notes
            )
            VALUES (
                :tenantId, :reviewTaskId, :reviewerId, :decisionVersion,
                :decision, :notes
            )
            """)
        .param("tenantId", context.tenantId())
        .param("reviewTaskId", reviewTask.id())
        .param("reviewerId", reviewTask.reviewerId())
        .param("decisionVersion", reviewTask.version())
        .param("decision", reviewTask.decision().orElseThrow().name())
        .param("notes", reviewTask.notes())
        .update();
    return reviewTask;
  }

  private QuoteReviewTask reviewTaskFromRow(
      java.sql.ResultSet resultSet, AiExecutionContext context) {
    try {
      String decision = resultSet.getString("decision");
      return new QuoteReviewTask(
          resultSet.getObject("id", UUID.class),
          context.tenantId(),
          resultSet.getObject("workflow_id", UUID.class),
          QuoteReviewStatus.valueOf(resultSet.getString("status")),
          resultSet.getObject("reviewer_id", UUID.class),
          decision == null
              ? Optional.empty()
              : Optional.of(QuoteReviewDecisionType.valueOf(decision)),
          resultSet.getString("notes"),
          objectMapper.readValue(resultSet.getString("uncertainty_reasons"), REASONS_TYPE),
          resultSet.getLong("version"));
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to deserialize quote review task", exception);
    }
  }

  private static AiExecutionContext requireContext(QuoteReviewTask reviewTask) {
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    if (!context.tenantId().equals(reviewTask.tenantId())) {
      throw new IllegalArgumentException("tenantId does not match AI execution context");
    }
    if (!context.workflowId().equals(reviewTask.workflowId())) {
      throw new IllegalArgumentException("workflowId does not match AI execution context");
    }
    return context;
  }
}
