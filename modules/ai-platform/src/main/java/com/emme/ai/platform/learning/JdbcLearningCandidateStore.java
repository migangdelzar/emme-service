package com.emme.ai.platform.learning;

import com.emme.ai.contracts.learning.LearningCandidate;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/** PostgreSQL adapter for tenant-scoped, reviewable learning candidates. */
public final class JdbcLearningCandidateStore implements LearningCandidateStore {

  private final JdbcClient jdbc;
  private final ObjectMapper objectMapper;

  public JdbcLearningCandidateStore(JdbcClient jdbc, ObjectMapper objectMapper) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public UUID save(LearningCandidate candidate, AiExecutionContext context) {
    Objects.requireNonNull(candidate, "candidate must not be null");
    Objects.requireNonNull(context, "context must not be null");
    AiExecutionContext boundContext = AiExecutionContextScope.requireCurrent();
    if (!boundContext.equals(context)) {
      throw new SecurityException("Learning candidate context does not match the bound AI context");
    }

    return jdbc.sql(
            """
            INSERT INTO ai_learning_candidate (
                tenant_id, principal_id, conversation_id, workflow_id, trace_id,
                candidate_key, candidate_kind, reference_text, reference_fingerprint,
                locale, embedding_model_version, evidence, status
            )
            VALUES (
                :tenantId, :principalId, :conversationId, :workflowId, :traceId,
                :candidateKey, :candidateKind, :referenceText, :referenceFingerprint,
                :locale, :embeddingModelVersion, CAST(:evidence AS jsonb), 'PENDING_EVALUATION'
            )
            ON CONFLICT (
                tenant_id, principal_id, candidate_key, reference_fingerprint,
                embedding_model_version
            ) DO UPDATE SET updated_at = ai_learning_candidate.updated_at
            RETURNING id
            """)
        .param("tenantId", boundContext.tenantId())
        .param("principalId", boundContext.principalId())
        .param("conversationId", boundContext.conversationId())
        .param("workflowId", boundContext.workflowId())
        .param("traceId", boundContext.traceId())
        .param("candidateKey", candidate.candidateKey())
        .param("candidateKind", candidate.kind().name())
        .param("referenceText", candidate.referenceText())
        .param("referenceFingerprint", fingerprint(candidate.referenceText()))
        .param("locale", candidate.locale())
        .param("embeddingModelVersion", candidate.embeddingModelVersion())
        .param("evidence", evidence(candidate))
        .query((resultSet, rowNumber) -> resultSet.getObject("id", UUID.class))
        .single();
  }

  private String evidence(LearningCandidate candidate) {
    try {
      return objectMapper.writeValueAsString(candidate.evidence());
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(
          "Learning candidate evidence could not be serialized", exception);
    }
  }

  private static String fingerprint(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required by the runtime", exception);
    }
  }
}
