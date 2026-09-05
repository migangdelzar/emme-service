package com.emme.assistant.adapter.out.persistence.repository;

import com.emme.assistant.adapter.out.persistence.entity.ChannelParticipantEntity;
import com.emme.kernel.type.ChannelType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataChannelParticipantRepository
    extends JpaRepository<ChannelParticipantEntity, UUID> {
  Optional<ChannelParticipantEntity> findByTenantIdAndChannelAndProviderReference(
      UUID tenantId, ChannelType channel, String providerReference);
}
