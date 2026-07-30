package com.emme.assistant.entity;

import com.emme.kernel.type.ChannelType;
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
@Table(name = "conversation")
public class Conversation extends TenantOwnedEntity {

  @Column(name = "participant_id", nullable = false)
  private UUID participantId;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false, length = 20)
  private ChannelType channel;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ConversationStatus status = ConversationStatus.ACTIVE;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  protected Conversation() {}

  public Conversation(UUID tenantId, UUID participantId, ChannelType channel) {
    super(tenantId);
    this.participantId = Objects.requireNonNull(participantId, "participantId must not be null");
    this.channel = Objects.requireNonNull(channel, "channel must not be null");
    this.startedAt = Instant.now();
  }

  public UUID getParticipantId() {
    return participantId;
  }

  public ChannelType getChannel() {
    return channel;
  }

  public ConversationStatus getStatus() {
    return status;
  }

  public void setStatus(ConversationStatus status) {
    this.status = status;
  }

  public Instant getStartedAt() {
    return startedAt;
  }
}
