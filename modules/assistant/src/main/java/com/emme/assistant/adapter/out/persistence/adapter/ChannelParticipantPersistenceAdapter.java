package com.emme.assistant.adapter.out.persistence.adapter;

import com.emme.assistant.adapter.out.persistence.entity.ChannelParticipantEntity;
import com.emme.assistant.adapter.out.persistence.mapper.ChannelParticipantPersistenceMapper;
import com.emme.assistant.adapter.out.persistence.repository.SpringDataChannelParticipantRepository;
import com.emme.assistant.application.port.out.ChannelParticipantRepository;
import com.emme.assistant.domain.model.ChannelParticipant;
import com.emme.kernel.type.ChannelType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ChannelParticipantPersistenceAdapter implements ChannelParticipantRepository {
  private final SpringDataChannelParticipantRepository repository;
  private final ChannelParticipantPersistenceMapper mapper;

  public ChannelParticipantPersistenceAdapter(
      SpringDataChannelParticipantRepository repository,
      ChannelParticipantPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<ChannelParticipant> findByTenantIdAndChannelAndProviderReference(
      UUID tenantId, ChannelType channel, String providerReference) {
    return repository
        .findByTenantIdAndChannelAndProviderReference(tenantId, channel, providerReference)
        .map(mapper::toDomain);
  }

  @Override
  public ChannelParticipant save(ChannelParticipant participant) {
    ChannelParticipantEntity saved = repository.save(mapper.toEntity(participant));
    return mapper.toDomain(saved);
  }
}
