package com.emme.assistant.adapter.out.persistence.entity;

import com.emme.shared.persistence.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "conversation_event",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"conversation_id", "sequence_number"})})
public class ConversationEventEntity extends TenantOwnedEntity {

  @Column(name = "conversation_id", nullable = false)
  private UUID conversationId;

  @Column(name = "sequence_number", nullable = false)
  private Integer sequenceNumber;

  @Column(name = "event_type", nullable = false, length = 80)
  private String eventType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
  private String payload;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "idempotency_key", length = 255)
  private String idempotencyKey;

  @Column(name = "idempotency_principal_id")
  private UUID idempotencyPrincipalId;

  protected ConversationEventEntity() {}

  public ConversationEventEntity(
      UUID tenantId,
      UUID conversationId,
      Integer sequenceNumber,
      String eventType,
      String payload,
      String idempotencyKey,
      UUID idempotencyPrincipalId) {
    super(tenantId);
    this.conversationId = Objects.requireNonNull(conversationId, "conversationId must not be null");
    this.sequenceNumber = Objects.requireNonNull(sequenceNumber, "sequenceNumber must not be null");
    this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
    this.payload = Objects.requireNonNull(payload, "payload must not be null");
    this.idempotencyKey = idempotencyKey;
    this.idempotencyPrincipalId = idempotencyPrincipalId;
    this.occurredAt = Instant.now();
  }

  public ConversationEventEntity(
      UUID tenantId,
      UUID conversationId,
      Integer sequenceNumber,
      String eventType,
      String payload,
      String idempotencyKey) {
    this(tenantId, conversationId, sequenceNumber, eventType, payload, idempotencyKey, null);
  }

  public ConversationEventEntity(
      UUID tenantId,
      UUID conversationId,
      Integer sequenceNumber,
      String eventType,
      String payload) {
    this(tenantId, conversationId, sequenceNumber, eventType, payload, null);
  }

  public UUID getConversationId() {
    return conversationId;
  }

  public Integer getSequenceNumber() {
    return sequenceNumber;
  }

  public String getEventType() {
    return eventType;
  }

  public String getPayload() {
    return payload;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public UUID getIdempotencyPrincipalId() {
    return idempotencyPrincipalId;
  }

  public void restoreIdentity(UUID id, Instant occurredAt) {
    restoreAuditFields(id, occurredAt, occurredAt);
  }
}
