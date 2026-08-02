package com.emme.assistant.application.port.out;

import com.emme.assistant.domain.model.ConversationEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationEventRepository {
  Optional<ConversationEvent> findLatestByTenantIdAndConversationId(
      UUID tenantId, UUID conversationId);

  List<ConversationEvent> findByTenantIdAndConversationId(UUID tenantId, UUID conversationId);

  ConversationEvent save(ConversationEvent event);
}
