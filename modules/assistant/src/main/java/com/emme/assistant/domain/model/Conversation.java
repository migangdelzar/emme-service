package com.emme.assistant.domain.model;

import com.emme.kernel.type.ChannelType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Framework-free conversation aggregate. */
public final class Conversation {
  private final UUID id;
  private final UUID tenantId;
  private final UUID participantId;
  private final ChannelType channel;
  private final Instant startedAt;
  private ConversationStatus status;

  public Conversation(UUID tenantId, UUID participantId, ChannelType channel) {
    this(
        UUID.randomUUID(),
        tenantId,
        participantId,
        channel,
        ConversationStatus.ACTIVE,
        Instant.now());
  }

  private Conversation(
      UUID id,
      UUID tenantId,
      UUID participantId,
      ChannelType channel,
      ConversationStatus status,
      Instant startedAt) {
    this.id = Objects.requireNonNull(id);
    this.tenantId = Objects.requireNonNull(tenantId);
    this.participantId = Objects.requireNonNull(participantId);
    this.channel = Objects.requireNonNull(channel);
    this.status = Objects.requireNonNull(status);
    this.startedAt = Objects.requireNonNull(startedAt);
  }

  public static Conversation rehydrate(
      UUID id,
      UUID tenantId,
      UUID participantId,
      ChannelType channel,
      ConversationStatus status,
      Instant startedAt) {
    return new Conversation(id, tenantId, participantId, channel, status, startedAt);
  }

  public UUID id() {
    return id;
  }

  public UUID tenantId() {
    return tenantId;
  }

  public UUID participantId() {
    return participantId;
  }

  public ChannelType channel() {
    return channel;
  }

  public Instant startedAt() {
    return startedAt;
  }

  public ConversationStatus status() {
    return status;
  }

  public void close() {
    if (status != ConversationStatus.ACTIVE) {
      throw new IllegalStateException("Conversation is not active: " + id);
    }
    status = ConversationStatus.CLOSED;
  }
}
