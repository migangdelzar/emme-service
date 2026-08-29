package com.emme.ai.platform.learning;

import com.emme.ai.contracts.learning.LearningCandidateStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** PostgreSQL adapter for tenant-filtered candidate lifecycle updates. */
@Component
public final class JdbcLearningCandidateStateStore implements LearningCandidateStateStore {

  private final JdbcClient jdbc;

  public JdbcLearningCandidateStateStore(JdbcClient jdbc) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
  }

  @Override
  public Optional<LearningCandidateState> find(UUID candidateId) {
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    return jdbc
        .sql(
            """
            SELECT status, version
            FROM ai_learning_candidate
            WHERE id = :candidateId
              AND tenant_id = :tenantId
            """)
        .param("candidateId", candidateId)
        .param("tenantId", context.tenantId())
        .query(
            (resultSet, rowNumber) ->
                new LearningCandidateState(
                    candidateId,
                    LearningCandidateStatus.valueOf(resultSet.getString("status")),
                    resultSet.getLong("version")))
        .list()
        .stream()
        .findFirst();
  }

  @Override
  public boolean transition(
      UUID candidateId,
      LearningCandidateStatus expectedStatus,
      long expectedVersion,
      LearningCandidateStatus targetStatus,
      String reason) {
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(expectedStatus, "expectedStatus must not be null");
    Objects.requireNonNull(targetStatus, "targetStatus must not be null");
    Objects.requireNonNull(reason, "reason must not be null");
    if (expectedVersion < 0) {
      throw new IllegalArgumentException("expectedVersion must not be negative");
    }
    if (reason.isBlank()) {
      throw new IllegalArgumentException("reason must not be blank");
    }
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    return jdbc.sql(
                """
                UPDATE ai_learning_candidate
                SET status = :targetStatus,
                    decision_reason = :reason,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE id = :candidateId
                  AND tenant_id = :tenantId
                  AND status = :expectedStatus
                  AND version = :expectedVersion
                """)
            .param("targetStatus", targetStatus.name())
            .param("reason", reason)
            .param("candidateId", candidateId)
            .param("tenantId", context.tenantId())
            .param("expectedStatus", expectedStatus.name())
            .param("expectedVersion", expectedVersion)
            .update()
        == 1;
  }
}
