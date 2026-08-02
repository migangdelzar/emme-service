package com.emme.assistant.adapter.out.persistence.entity;

import com.emme.shared.persistence.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

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

  @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
  private String payload;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  protected ConversationEventEntity() {}

  public ConversationEventEntity(
      UUID tenantId,
      UUID conversationId,
      Integer sequenceNumber,
      String eventType,
      String payload) {
    super(tenantId);
    this.conversationId = Objects.requireNonNull(conversationId, "conversationId must not be null");
    this.sequenceNumber = Objects.requireNonNull(sequenceNumber, "sequenceNumber must not be null");
    this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
    this.payload = Objects.requireNonNull(payload, "payload must not be null");
    this.occurredAt = Instant.now();
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

  public void restoreIdentity(UUID id, Instant occurredAt) {
    restoreAuditFields(id, occurredAt, occurredAt);
  }
}
