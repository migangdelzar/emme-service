package com.emme.ai.platform.learning;

import com.emme.ai.contracts.learning.LearningCandidateEvaluationReport;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/** PostgreSQL adapter for tenant-scoped, idempotent offline evaluation evidence. */
public final class JdbcLearningCandidateEvaluationStore
    implements LearningCandidateEvaluationStore {

  private final JdbcClient jdbc;
  private final ObjectMapper objectMapper;

  public JdbcLearningCandidateEvaluationStore(JdbcClient jdbc, ObjectMapper objectMapper) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public UUID save(
      UUID candidateId, LearningCandidateEvaluationReport report, AiExecutionContext context) {
    Objects.requireNonNull(candidateId, "candidateId must not be null");
    Objects.requireNonNull(report, "report must not be null");
    Objects.requireNonNull(context, "context must not be null");
    AiExecutionContext boundContext = AiExecutionContextScope.requireCurrent();
    if (!boundContext.equals(context)) {
      throw new SecurityException(
          "Learning candidate evaluation context does not match the bound AI context");
    }

    return jdbc.sql(
            """
            INSERT INTO ai_learning_candidate_evaluation (
                tenant_id, candidate_id, evaluation_version,
                dataset_complete, safety_passed, regression_passed,
                shadow_comparison_passed, canary_passed, metrics
            )
            SELECT
                :tenantId, :candidateId, :evaluationVersion,
                :datasetComplete, :safetyPassed, :regressionPassed,
                :shadowComparisonPassed, :canaryPassed, CAST(:metrics AS jsonb)
            FROM ai_learning_candidate
            WHERE id = :candidateId
              AND tenant_id = :tenantId
            ON CONFLICT (tenant_id, candidate_id, evaluation_version)
            DO UPDATE SET updated_at = ai_learning_candidate_evaluation.updated_at
            RETURNING id
            """)
        .param("tenantId", boundContext.tenantId())
        .param("candidateId", candidateId)
        .param("evaluationVersion", report.evaluationVersion())
        .param("datasetComplete", report.datasetComplete())
        .param("safetyPassed", report.safetyPassed())
        .param("regressionPassed", report.regressionPassed())
        .param("shadowComparisonPassed", report.shadowComparisonPassed())
        .param("canaryPassed", report.canaryPassed())
        .param("metrics", metrics(report))
        .query((resultSet, rowNumber) -> resultSet.getObject("id", UUID.class))
        .single();
  }

  private String metrics(LearningCandidateEvaluationReport report) {
    try {
      return objectMapper.writeValueAsString(report.metrics());
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(
          "Learning candidate metrics could not be serialized", exception);
    }
  }
}
