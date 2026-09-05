package com.emme.assistant.adapter.out.persistence.repository;

import com.emme.assistant.adapter.out.persistence.entity.PendingActionEntity;
import com.emme.assistant.domain.model.ActionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataPendingActionRepository
    extends JpaRepository<PendingActionEntity, UUID> {
  List<PendingActionEntity> findByConversationIdAndStatusOrderByCreatedAtAscIdAsc(
      UUID conversationId, ActionStatus status);

  List<PendingActionEntity> findByExpiresAtBeforeAndStatus(Instant now, ActionStatus status);
}
