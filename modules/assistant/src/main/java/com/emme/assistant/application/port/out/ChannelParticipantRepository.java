package com.emme.assistant.application.port.out;

import com.emme.assistant.domain.model.ChannelParticipant;
import com.emme.kernel.type.ChannelType;
import java.util.Optional;
import java.util.UUID;

/** Outbound persistence capability for channel participants. */
public interface ChannelParticipantRepository {
  Optional<ChannelParticipant> findByTenantIdAndChannelAndProviderReference(
      UUID tenantId, ChannelType channel, String providerReference);

  ChannelParticipant save(ChannelParticipant participant);
}
