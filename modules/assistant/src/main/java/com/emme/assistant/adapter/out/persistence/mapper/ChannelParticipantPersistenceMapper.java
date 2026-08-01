package com.emme.assistant.adapter.out.persistence.mapper;

import com.emme.assistant.adapter.out.persistence.entity.ChannelParticipantEntity;
import com.emme.assistant.domain.model.ChannelParticipant;
import org.springframework.stereotype.Component;

@Component
public class ChannelParticipantPersistenceMapper {
  public ChannelParticipant toDomain(ChannelParticipantEntity entity) {
    return new ChannelParticipant(
        entity.getId(),
        entity.getTenantId(),
        entity.getChannel(),
        entity.getProviderReference(),
        entity.getCustomerId(),
        entity.getConsentStatus(),
        entity.getCreatedAt());
  }

  public ChannelParticipantEntity toEntity(ChannelParticipant participant) {
    ChannelParticipantEntity entity =
        new ChannelParticipantEntity(
            participant.tenantId(), participant.channel(), participant.providerReference());
    if (participant.id() != null) {
      entity.restoreIdentity(participant.id(), participant.createdAt());
    }
    entity.setCustomerId(participant.customerId());
    entity.setConsentStatus(participant.consentStatus());
    return entity;
  }
}
