package com.emme.assistant.entity;

import com.emme.kernel.type.ChannelType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChannelParticipantRepository extends JpaRepository<ChannelParticipant, UUID> {
  List<ChannelParticipant> findByTenantId(UUID tenantId);

  Optional<ChannelParticipant> findByTenantIdAndChannelAndProviderReference(
      UUID tenantId, ChannelType channel, String providerReference);
}
