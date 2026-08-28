package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.assistant.ai.application.port.out.NailDesignExtractor;
import com.emme.assistant.ai.application.port.out.QuoteArtifactRepository;
import com.emme.assistant.ai.domain.quote.QuoteCalculation;
import com.emme.assistant.ai.domain.quote.QuoteLineType;
import com.emme.assistant.ai.domain.workflow.QuoteDraft;
import com.emme.assistant.ai.domain.workflow.QuoteReviewStatus;
import com.emme.assistant.ai.domain.workflow.QuoteReviewTask;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** PostgreSQL adapter for extracted design, quote draft, and review artifacts. */
@Component
public final class JdbcQuoteArtifactRepository implements QuoteArtifactRepository {

  private final JdbcClient jdbc;
  private final ObjectMapper objectMapper;

  public JdbcQuoteArtifactRepository(JdbcClient jdbc, ObjectMapper objectMapper) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public void saveExtraction(UUID workflowId, NailDesignExtractor.ExtractionResult extraction) {
    Objects.requireNonNull(extraction, "extraction must not be null");
    AiExecutionContext context = contextFor(workflowId);
    jdbc.sql(
            """
            INSERT INTO ai_extraction_result (
                tenant_id, workflow_id, model_version, prompt_version, schema_version,
                attributes, confidence_by_field, ambiguities, validation_status,
                needs_human_review
            )
            VALUES (
                :tenantId, :workflowId, :modelVersion, :promptVersion, :schemaVersion,
                CAST(:attributes AS jsonb), CAST(:confidenceByField AS jsonb),
                CAST(:ambiguities AS jsonb), 'VALID', :needsHumanReview
            )
            ON CONFLICT (tenant_id, workflow_id)
            DO UPDATE SET
                model_version = EXCLUDED.model_version,
                prompt_version = EXCLUDED.prompt_version,
                schema_version = EXCLUDED.schema_version,
                attributes = EXCLUDED.attributes,
                confidence_by_field = EXCLUDED.confidence_by_field,
                ambiguities = EXCLUDED.ambiguities,
                validation_status = EXCLUDED.validation_status,
                needs_human_review = EXCLUDED.needs_human_review,
                updated_at = CURRENT_TIMESTAMP,
                version = ai_extraction_result.version + 1
            """)
        .param("tenantId", context.tenantId())
        .param("workflowId", workflowId)
        .param("modelVersion", extraction.modelVersion())
        .param("promptVersion", extraction.promptVersion())
        .param("schemaVersion", extraction.schemaVersion())
        .param("attributes", json(extraction.features()))
        .param("confidenceByField", json(extraction.features().confidenceByField()))
        .param("ambiguities", json(extraction.features().ambiguities()))
        .param("needsHumanReview", extraction.features().needsHumanReview())
        .update();
  }

  @Override
  public void saveDraft(QuoteDraft draft) {
    Objects.requireNonNull(draft, "draft must not be null");
    AiExecutionContext context = contextFor(draft.workflowId());
    if (!context.tenantId().equals(draft.tenantId())) {
      throw new IllegalArgumentException("tenantId does not match AI execution context");
    }
    QuoteCalculation calculation = draft.calculation();
    jdbc.sql(
            """
            INSERT INTO quote_draft (
                id, tenant_id, workflow_id, extraction_result_id, template_key,
                template_version, required_services, add_ons, min_price, max_price,
                duration_minutes, currency, status
            )
            VALUES (
                :draftId, :tenantId, :workflowId,
                (SELECT id FROM ai_extraction_result
                 WHERE tenant_id = :tenantId AND workflow_id = :workflowId),
                :templateKey, :templateVersion, CAST(:requiredServices AS jsonb),
                CAST(:addOns AS jsonb), :minPrice, :maxPrice, :durationMinutes,
                :currency, :status
            )
            ON CONFLICT (tenant_id, workflow_id)
            DO UPDATE SET
                template_key = EXCLUDED.template_key,
                template_version = EXCLUDED.template_version,
                required_services = EXCLUDED.required_services,
                add_ons = EXCLUDED.add_ons,
                min_price = EXCLUDED.min_price,
                max_price = EXCLUDED.max_price,
                duration_minutes = EXCLUDED.duration_minutes,
                currency = EXCLUDED.currency,
                status = EXCLUDED.status,
                updated_at = CURRENT_TIMESTAMP,
                version = quote_draft.version + 1
            """)
        .param("draftId", draft.id())
        .param("tenantId", context.tenantId())
        .param("workflowId", draft.workflowId())
        .param("templateKey", calculation.templateKey())
        .param("templateVersion", calculation.templateVersion())
        .param("requiredServices", json(lineCodes(calculation, QuoteLineType.REQUIRED_SERVICE)))
        .param("addOns", json(lineCodes(calculation, QuoteLineType.ADD_ON)))
        .param("minPrice", calculation.minimumPrice())
        .param("maxPrice", calculation.maximumPrice())
        .param("durationMinutes", calculation.durationMinutes())
        .param("currency", calculation.currency())
        .param("status", calculation.needsHumanReview() ? "PENDING_REVIEW" : "READY")
        .update();
  }

  @Override
  public QuoteReviewTask saveReviewTask(QuoteReviewTask reviewTask) {
    Objects.requireNonNull(reviewTask, "reviewTask must not be null");
    AiExecutionContext context = contextFor(reviewTask.workflowId());
    if (!context.tenantId().equals(reviewTask.tenantId())) {
      throw new IllegalArgumentException("tenantId does not match AI execution context");
    }
    return jdbc.sql(
            """
            INSERT INTO quote_review_task (
                id, tenant_id, workflow_id, quote_draft_id, status, uncertainty_reasons
            )
            VALUES (
                :reviewTaskId, :tenantId, :workflowId,
                (SELECT id FROM quote_draft
                 WHERE tenant_id = :tenantId AND workflow_id = :workflowId),
                :status, CAST(:uncertaintyReasons AS jsonb)
            )
            ON CONFLICT (tenant_id, workflow_id)
            DO UPDATE SET updated_at = CURRENT_TIMESTAMP
            RETURNING id, status, reviewer_id, version
            """)
        .param("reviewTaskId", reviewTask.id())
        .param("tenantId", context.tenantId())
        .param("workflowId", reviewTask.workflowId())
        .param("status", reviewTask.status().name())
        .param("uncertaintyReasons", json(reviewTask.uncertaintyReasons()))
        .query(
            (resultSet, rowNumber) ->
                new QuoteReviewTask(
                    resultSet.getObject("id", UUID.class),
                    context.tenantId(),
                    reviewTask.workflowId(),
                    QuoteReviewStatus.valueOf(resultSet.getString("status")),
                    resultSet.getObject("reviewer_id", UUID.class),
                    Optional.empty(),
                    null,
                    reviewTask.uncertaintyReasons(),
                    resultSet.getLong("version")))
        .single();
  }

  private AiExecutionContext contextFor(UUID workflowId) {
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    if (!context.workflowId().equals(workflowId)) {
      throw new IllegalArgumentException("workflowId does not match AI execution context");
    }
    return context;
  }

  private static List<String> lineCodes(QuoteCalculation calculation, QuoteLineType type) {
    return calculation.appliedLines().stream()
        .filter(line -> line.type() == type)
        .map(line -> line.code())
        .toList();
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to serialize quote artifact", exception);
    }
  }
}
