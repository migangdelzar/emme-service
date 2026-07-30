package com.emme.assistant.entity;

import com.emme.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "pending_action")
public class PendingAction extends TenantOwnedEntity {

  @Column(name = "conversation_id", nullable = false)
  private UUID conversationId;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_type", nullable = false, length = 20)
  private ActionType actionType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ActionStatus status = ActionStatus.PENDING;

  @Column(name = "details", columnDefinition = "jsonb")
  private String details;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at_override", nullable = false)
  private Instant createdAtOverride;

  protected PendingAction() {}

  public PendingAction(
      UUID tenantId,
      UUID conversationId,
      ActionType actionType,
      String details,
      Instant expiresAt) {
    super(tenantId);
    this.conversationId = Objects.requireNonNull(conversationId, "conversationId must not be null");
    this.actionType = Objects.requireNonNull(actionType, "actionType must not be null");
    this.details = details;
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    this.createdAtOverride = Instant.now();
  }

  public UUID getConversationId() {
    return conversationId;
  }

  public ActionType getActionType() {
    return actionType;
  }

  public ActionStatus getStatus() {
    return status;
  }

  public void setStatus(ActionStatus status) {
    this.status = status;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getCreatedAtOverride() {
    return createdAtOverride;
  }
}
