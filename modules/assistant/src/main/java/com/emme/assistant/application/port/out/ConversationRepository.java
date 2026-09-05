package com.emme.assistant.application.port.out;

import com.emme.assistant.domain.model.Conversation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository {
  Optional<Conversation> findById(UUID conversationId);

  List<Conversation> findByTenantId(UUID tenantId);

  Conversation save(Conversation conversation);
}
