package com.emme.assistant.application.port.out;

import com.emme.assistant.domain.model.ActionStatus;
import com.emme.assistant.domain.model.PendingAction;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PendingActionRepository {
  Optional<PendingAction> findById(UUID actionId);

  List<PendingAction> findByTenantIdAndConversationIdAndStatus(
      UUID tenantId, UUID conversationId, ActionStatus status);

  List<PendingAction> findExpired(Instant now, ActionStatus status);

  PendingAction save(PendingAction action);
}
