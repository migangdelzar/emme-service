package com.emme.assistant.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingActionRepository extends JpaRepository<PendingAction, UUID> {
  List<PendingAction> findByConversationIdAndStatus(UUID conversationId, ActionStatus status);

  List<PendingAction> findByExpiresAtBeforeAndStatus(Instant now, ActionStatus status);
}
