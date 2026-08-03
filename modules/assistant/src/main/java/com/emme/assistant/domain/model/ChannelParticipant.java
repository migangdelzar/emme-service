package com.emme.assistant.domain.model;

import com.emme.kernel.type.ChannelType;
import java.time.Instant;
import java.util.UUID;

public record ChannelParticipant(
    UUID id,
    UUID tenantId,
    ChannelType channel,
    String providerReference,
    UUID customerId,
    ConsentStatus consentStatus,
    Instant createdAt) {
  public ChannelParticipant(
      UUID id,
      UUID tenantId,
      ChannelType channel,
      String providerReference,
      UUID customerId,
      ConsentStatus consentStatus) {
    this(id, tenantId, channel, providerReference, customerId, consentStatus, Instant.now());
  }
}
