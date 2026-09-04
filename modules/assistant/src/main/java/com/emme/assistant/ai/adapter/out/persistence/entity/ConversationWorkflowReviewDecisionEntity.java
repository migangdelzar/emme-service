package com.emme.assistant.ai.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** JPA mapping for the append-only conversation workflow review audit. */
@Entity
@Table(name = "ai_conversation_workflow_review_decision")
public class ConversationWorkflowReviewDecisionEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "workflow_id", nullable = false, updatable = false)
  private UUID workflowId;

  @Column(name = "conversation_id", nullable = false, updatable = false)
  private UUID conversationId;

  @Column(name = "reviewer_id", nullable = false, updatable = false)
  private UUID reviewerId;

  @Column(name = "decision", nullable = false, length = 40, updatable = false)
  private String decision;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "clarification", columnDefinition = "jsonb", updatable = false)
  private String clarification;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected ConversationWorkflowReviewDecisionEntity() {}

  public ConversationWorkflowReviewDecisionEntity(
      UUID tenantId,
      UUID workflowId,
      UUID conversationId,
      UUID reviewerId,
      String decision,
      String clarification) {
    this.id = UUID.randomUUID();
    this.tenantId = tenantId;
    this.workflowId = workflowId;
    this.conversationId = conversationId;
    this.reviewerId = reviewerId;
    this.decision = decision;
    this.clarification = clarification;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getWorkflowId() {
    return workflowId;
  }

  public UUID getConversationId() {
    return conversationId;
  }

  public UUID getReviewerId() {
    return reviewerId;
  }

  public String getDecision() {
    return decision;
  }

  public String getClarification() {
    return clarification;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
