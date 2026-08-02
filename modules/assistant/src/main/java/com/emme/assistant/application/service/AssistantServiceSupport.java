package com.emme.assistant.application.service;

import com.emme.assistant.api.exception.ConversationNotFoundException;
import com.emme.assistant.api.exception.PendingActionNotFoundException;
import com.emme.assistant.application.port.out.ConversationRepository;
import com.emme.assistant.application.port.out.PendingActionRepository;
import com.emme.assistant.domain.model.Conversation;
import com.emme.assistant.domain.model.PendingAction;
import java.util.UUID;

final class AssistantServiceSupport {
  private AssistantServiceSupport() {}

  static Conversation conversation(ConversationRepository repository, UUID tenantId, UUID id) {
    return repository
        .findByTenantIdAndId(tenantId, id)
        .orElseThrow(() -> new ConversationNotFoundException(id));
  }

  static PendingAction action(PendingActionRepository repository, UUID tenantId, UUID id) {
    return repository
        .findByTenantIdAndId(tenantId, id)
        .orElseThrow(() -> new PendingActionNotFoundException(id));
  }
}
